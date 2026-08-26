package com.cappleapple.presencenotposition.client.music;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.music.DayPeriod;
import com.cappleapple.presencenotposition.music.MusicChoice;
import com.cappleapple.presencenotposition.music.MusicContextResolver;
import com.cappleapple.presencenotposition.music.MusicDefinition;
import com.cappleapple.presencenotposition.music.NormalizationMetadata;
import com.cappleapple.presencenotposition.music.TrackSelector;
import com.cappleapple.presencenotposition.network.ContextPayload;
import com.cappleapple.presencenotposition.resource.ClientResourceIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ClientMusicManager {
    private static final Random RANDOM = new Random();
    private static final Set<LocationContext> ACTIVE_CONTEXTS = new HashSet<>();
    private static final Map<SelectorKey, TrackSelector> SELECTORS = new HashMap<>();
    private static final Map<SelectorKey, Playback> SUSPENDED = new HashMap<>();
    private static final List<Playback> FADING = new ArrayList<>();
    private static DayPeriod period;
    private static DayPeriod periodCandidate;
    private static long periodCandidateSince;
    private static MusicChoice resolvedCandidate;
    private static long resolvedCandidateSince;
    private static MusicChoice winner;
    private static Playback primary;
    private static long scheduledStartTick = Long.MAX_VALUE;
    private static long nextTrackTick = Long.MAX_VALUE;
    private static long resourceRevision = -1;
    private static boolean manualStop;

    private ClientMusicManager() {
    }

    public static void receive(ContextPayload payload) {
        if (payload.entered()) ACTIVE_CONTEXTS.add(payload.context());
        else ACTIVE_CONTEXTS.remove(payload.context());
        manualStop = false;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            VanillaMusicController.tick(false);
            return;
        }
        long tick = com.cappleapple.presencenotposition.client.ClientPresentationManager.clientTick();
        updatePeriod(minecraft, tick);
        ClientResourceIndex.Snapshot resources = ClientResourceIndex.snapshot();
        if (resourceRevision != resources.revision()) {
            resourceRevision = resources.revision();
            SELECTORS.clear();
            manualStop = false;
            resolvedCandidate = null;
        }

        Optional<MusicChoice> resolved = ClientConfig.MUSIC_ENABLED.get()
            ? MusicContextResolver.resolve(ACTIVE_CONTEXTS, resources.music(), context -> ClientConfig.musicEnabled(context.type()), period)
            : Optional.empty();
        updateWinner(resolved.orElse(null), tick);
        refreshTitleWait(tick);
        tickFades(minecraft, tick);
        removeFinishedSuspended(minecraft);

        if (!manualStop && winner != null && tick >= scheduledStartTick && !sameChoice(primary, winner)) {
            transitionTo(winner, minecraft, tick, resources);
        }
        if (!manualStop && winner != null && primary == null && tick >= nextTrackTick && !winner.silence()) {
            startNewPlayback(winner, minecraft, tick, resources, false);
        }
        if (primary != null && tick - primary.startedTick > 10 && !minecraft.getSoundManager().isActive(primary.sound)) {
            MusicChoice finishedChoice = primary.choice;
            primary = null;
            nextTrackTick = tick + finishedChoice.definition().trackDelay().randomTicks(RANDOM);
        }
        VanillaMusicController.tick(winner != null && !manualStop);
    }

    private static void updatePeriod(Minecraft minecraft, long tick) {
        DayPeriod observed = minecraft.level.isDay() ? DayPeriod.DAY : DayPeriod.NIGHT;
        if (period == null) {
            period = observed;
            return;
        }
        if (observed == period) {
            periodCandidate = null;
        } else if (observed != periodCandidate) {
            periodCandidate = observed;
            periodCandidateSince = tick;
        } else if (tick - periodCandidateSince >= 40) {
            period = observed;
            periodCandidate = null;
            resolvedCandidate = null;
        }
    }

    private static void updateWinner(@Nullable MusicChoice next, long tick) {
        if (Objects.equals(next, winner)) {
            resolvedCandidate = null;
            return;
        }
        if (!Objects.equals(next, resolvedCandidate)) {
            resolvedCandidate = next;
            resolvedCandidateSince = tick;
        }
        boolean dayNightOnly = next != null && winner != null && next.context().equals(winner.context()) && next.period() != winner.period();
        int stability = next == null || dayNightOnly ? 0 : next.definition().transitionDelayTicks(next.context().type());
        if (tick - resolvedCandidateSince < stability) return;
        winner = next;
        resolvedCandidate = null;
        if (next == null) {
            scheduledStartTick = Long.MAX_VALUE;
            fadePrimary(false, tick);
            nextTrackTick = Long.MAX_VALUE;
            return;
        }
        long titleEnd = next.definition().startAfterTitle()
            ? com.cappleapple.presencenotposition.client.ClientPresentationManager.musicNotBefore(next.context())
            : tick;
        scheduledStartTick = Math.max(titleEnd, tick) + MusicDefinition.secondsToTicks(next.definition().startDelaySeconds());
        nextTrackTick = scheduledStartTick;
    }

    private static void refreshTitleWait(long tick) {
        if (winner == null || !winner.definition().startAfterTitle() || primary != null || scheduledStartTick != Long.MAX_VALUE) return;
        long titleEnd = com.cappleapple.presencenotposition.client.ClientPresentationManager.musicNotBefore(winner.context());
        if (titleEnd != Long.MAX_VALUE) {
            scheduledStartTick = Math.max(titleEnd, tick) + MusicDefinition.secondsToTicks(winner.definition().startDelaySeconds());
            nextTrackTick = scheduledStartTick;
        }
    }

    private static void transitionTo(MusicChoice choice, Minecraft minecraft, long tick, ClientResourceIndex.Snapshot resources) {
        if (choice.silence()) {
            fadePrimary(true, tick);
            primary = null;
            scheduledStartTick = Long.MAX_VALUE;
            nextTrackTick = Long.MAX_VALUE;
            return;
        }
        SelectorKey key = new SelectorKey(choice.context(), choice.period());
        Playback resume = SUSPENDED.remove(key);
        fadePrimary(true, tick);
        if (resume != null && minecraft.getSoundManager().isActive(resume.sound)) {
            FADING.remove(resume);
            resume.choice = choice;
            resume.fade(tick, MusicDefinition.secondsToTicks(choice.definition().transitionFadeInSeconds()), resume.volume, targetVolume(choice, resume.track, resources), FadeEnd.NONE);
            primary = resume;
        } else {
            startNewPlayback(choice, minecraft, tick, resources, true);
        }
        scheduledStartTick = Long.MAX_VALUE;
    }

    private static void startNewPlayback(
        MusicChoice choice, Minecraft minecraft, long tick, ClientResourceIndex.Snapshot resources, boolean transition
    ) {
        if (choice.tracks().isEmpty()) return;
        SelectorKey key = new SelectorKey(choice.context(), choice.period());
        TrackSelector selector = SELECTORS.computeIfAbsent(key, ignored -> new TrackSelector(
            choice.tracks(), choice.definition().selection(), choice.definition().avoidImmediateRepeat(), RANDOM));
        ResourceLocation track = selector.next();
        if (track == null) return;
        DynamicMusicSound sound = new DynamicMusicSound(track);
        Playback playback = new Playback(choice, track, sound, tick);
        float target = targetVolume(choice, track, resources);
        double fadeSeconds = transition ? choice.definition().transitionFadeInSeconds() : choice.definition().fadeInSeconds();
        playback.fade(tick, MusicDefinition.secondsToTicks(fadeSeconds), 0.0F, target, FadeEnd.NONE);
        minecraft.getSoundManager().play(sound);
        PresenceNotPosition.LOGGER.info("Starting location music {} for {}", track, choice.context());
        primary = playback;
        nextTrackTick = Long.MAX_VALUE;
    }

    private static float targetVolume(MusicChoice choice, ResourceLocation track, ClientResourceIndex.Snapshot resources) {
        float normalization = 1.0F;
        if (choice.definition().normalizeVolume()) {
            normalization = resources.normalization().getOrDefault(track, NormalizationMetadata.UNKNOWN)
                .gainForTarget(choice.definition().normalizationTarget());
        }
        return (float) Math.max(0.0, choice.definition().volume() * ClientConfig.MUSIC_MASTER_VOLUME.get() * normalization);
    }

    private static void fadePrimary(boolean transition, long tick) {
        if (primary == null) return;
        Playback old = primary;
        primary = null;
        boolean canResume = old.choice.definition().resume() && ACTIVE_CONTEXTS.contains(old.choice.context());
        int ticks = MusicDefinition.secondsToTicks(transition
            ? old.choice.definition().transitionFadeOutSeconds()
            : old.choice.definition().fadeOutSeconds());
        old.fade(tick, ticks, old.volume, 0.0F, canResume ? FadeEnd.SUSPEND : FadeEnd.STOP);
        if (!FADING.contains(old)) FADING.add(old);
    }

    private static void tickFades(Minecraft minecraft, long tick) {
        if (primary != null) primary.tickFade(tick, minecraft);
        Iterator<Playback> iterator = FADING.iterator();
        while (iterator.hasNext()) {
            Playback playback = iterator.next();
            if (!playback.tickFade(tick, minecraft)) continue;
            iterator.remove();
            if (playback.fadeEnd == FadeEnd.SUSPEND && minecraft.getSoundManager().isActive(playback.sound)) {
                SUSPENDED.put(new SelectorKey(playback.choice.context(), playback.choice.period()), playback);
            } else if (playback.fadeEnd == FadeEnd.STOP) {
                minecraft.getSoundManager().stop(playback.sound);
            }
        }
    }

    private static void removeFinishedSuspended(Minecraft minecraft) {
        SUSPENDED.values().removeIf(playback -> !minecraft.getSoundManager().isActive(playback.sound));
    }

    private static boolean sameChoice(@Nullable Playback playback, MusicChoice choice) {
        return playback != null && playback.choice.context().equals(choice.context()) && playback.choice.period() == choice.period();
    }

    public static boolean suppressVanillaStarts() {
        return winner != null && !manualStop
            && ClientConfig.VANILLA_MUSIC_BEHAVIOR.get() == ClientConfig.VanillaMusicBehavior.REPLACE;
    }

    public static void next() {
        Minecraft minecraft = Minecraft.getInstance();
        manualStop = false;
        if (primary != null) minecraft.getSoundManager().stop(primary.sound);
        primary = null;
        nextTrackTick = com.cappleapple.presencenotposition.client.ClientPresentationManager.clientTick();
    }

    public static void stopManual() {
        manualStop = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (primary != null) minecraft.getSoundManager().stop(primary.sound);
        for (Playback playback : FADING) minecraft.getSoundManager().stop(playback.sound);
        for (Playback playback : SUSPENDED.values()) minecraft.getSoundManager().stop(playback.sound);
        primary = null;
        FADING.clear();
        SUSPENDED.clear();
    }

    public static Component debugComponent() {
        if (winner == null) return Component.translatable("commands.presencenotposition.music.none");
        String track = primary == null ? (winner.silence() ? "(intentional silence)" : "(waiting)") : primary.track.toString();
        return Component.translatable("commands.presencenotposition.music.current", winner.context().toString(), track);
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        if (primary != null) minecraft.getSoundManager().stop(primary.sound);
        FADING.forEach(playback -> minecraft.getSoundManager().stop(playback.sound));
        SUSPENDED.values().forEach(playback -> minecraft.getSoundManager().stop(playback.sound));
        ACTIVE_CONTEXTS.clear();
        SELECTORS.clear();
        SUSPENDED.clear();
        FADING.clear();
        primary = null;
        winner = null;
        resolvedCandidate = null;
        period = null;
        manualStop = false;
    }

    private record SelectorKey(LocationContext context, DayPeriod period) { }
    private enum FadeEnd { NONE, STOP, SUSPEND }

    private static final class Playback {
        private MusicChoice choice;
        private final ResourceLocation track;
        private final DynamicMusicSound sound;
        private final long startedTick;
        private float volume;
        private float fadeFrom;
        private float fadeTo;
        private long fadeStart;
        private long fadeEndTick;
        private FadeEnd fadeEnd = FadeEnd.NONE;

        private Playback(MusicChoice choice, ResourceLocation track, DynamicMusicSound sound, long startedTick) {
            this.choice = choice;
            this.track = track;
            this.sound = sound;
            this.startedTick = startedTick;
        }

        private void fade(long tick, int duration, float from, float to, FadeEnd end) {
            this.volume = from;
            this.fadeFrom = from;
            this.fadeTo = to;
            this.fadeStart = tick;
            this.fadeEndTick = tick + Math.max(1, duration);
            this.fadeEnd = end;
            this.sound.setDynamicVolume(from);
        }

        private boolean tickFade(long tick, Minecraft minecraft) {
            float progress = Math.clamp((float) (tick - this.fadeStart) / (this.fadeEndTick - this.fadeStart), 0.0F, 1.0F);
            this.volume = this.fadeFrom + (this.fadeTo - this.fadeFrom) * progress;
            this.sound.setDynamicVolume(this.volume);
            return progress >= 1.0F;
        }
    }
}
