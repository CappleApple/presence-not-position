package com.cappleapple.presencenotposition.client;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.api.LocationNames;
import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.network.PresentationPayload;
import com.cappleapple.presencenotposition.presentation.EntrySoundDefinition;
import com.cappleapple.presencenotposition.presentation.EntrySoundSelector;
import com.cappleapple.presencenotposition.presentation.PresentationDefinition;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import com.cappleapple.presencenotposition.presentation.PresentationPolicy;
import com.cappleapple.presencenotposition.presentation.PresentationStackLayout;
import com.cappleapple.presencenotposition.presentation.TitleDefinition;
import com.cappleapple.presencenotposition.presentation.VisualDefinition;
import com.cappleapple.presencenotposition.resource.ClientResourceIndex;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

public final class ClientPresentationManager {
    private static final int MAX_QUEUE_AGE_TICKS = 200;
    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final Set<LocationContext> DEDUPLICATION = new HashSet<>();
    private static final Map<LocationContext, Long> MUSIC_NOT_BEFORE = new HashMap<>();
    private static long clientTick;
    private static final List<Active> ACTIVE = new ArrayList<>();

    private ClientPresentationManager() {
    }

    public static void receive(PresentationPayload payload) {
        PresentationDefinition definition = definition(payload.context());
        if (!allowed(payload.context(), payload.override())) return;
        if (ACTIVE.stream().anyMatch(active -> active.context().equals(payload.context())) || !DEDUPLICATION.add(payload.context())) return;
        int priority = payload.override().priority() == null
            ? definition.title().priority()
            : payload.override().priority();
        QUEUE.add(new Pending(payload.context(), payload.override(), priority, clientTick));
    }

    public static void tick() {
        clientTick++;
        PresentationHistory.tick();
        ACTIVE.removeIf(active -> {
            boolean finished = clientTick >= active.endTick();
            if (finished) DEDUPLICATION.remove(active.context());
            return finished;
        });
        QUEUE.removeIf(pending -> {
            boolean stale = clientTick - pending.enqueuedTick() > MAX_QUEUE_AGE_TICKS;
            if (stale) DEDUPLICATION.remove(pending.context());
            return stale;
        });
        if (ACTIVE.isEmpty() && !QUEUE.isEmpty()) {
            long oldestBatch = QUEUE.stream().mapToLong(Pending::enqueuedTick).min().orElse(clientTick);
            if (clientTick > oldestBatch) startBatch(oldestBatch);
        }
    }

    private static void startBatch(long enqueuedTick) {
        List<Pending> batch = QUEUE.stream().filter(pending -> pending.enqueuedTick() == enqueuedTick)
            .sorted(Comparator.comparingInt((Pending pending) -> PresentationStackLayout.order(pending.context().type()))
                .thenComparing(Comparator.comparingInt(Pending::priority).reversed())
                .thenComparing(pending -> pending.context().id().toString()))
            .toList();
        QUEUE.removeIf(pending -> pending.enqueuedTick() == enqueuedTick);
        PresenceNotPosition.LOGGER.info("Starting presentation stack in top-to-bottom order: {}",
            batch.stream().map(Pending::context).toList());
        List<EntrySoundSelector.Candidate> sounds = batch.stream()
            .map(ClientPresentationManager::start)
            .map(active -> new EntrySoundSelector.Candidate(active.definition().entrySound(), active.override()))
            .toList();
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        EntrySoundSelector.select(sounds, id -> {
            if (id.equals(SoundManager.INTENTIONALLY_EMPTY_SOUND_LOCATION)) return true;
            var event = soundManager.getSoundEvent(id);
            return event != null && event.getWeight() > 0;
        }, ThreadLocalRandom.current()).ifPresent(ClientPresentationManager::playEntrySound);
    }

    private static Active start(Pending pending) {
        PresentationDefinition definition = definition(pending.context());
        TitleDefinition base = definition.title();
        int duration = pending.override().durationTicks() == null ? base.durationTicks() : pending.override().durationTicks();
        Component title = pending.override().title() != null ? pending.override().title() : title(pending.context(), base);
        Component subtitle = pending.override().subtitle() != null ? pending.override().subtitle() : subtitle(base);
        long end = clientTick + duration;
        Active active = new Active(pending.context(), definition, pending.override(), title, subtitle, clientTick, end);
        ACTIVE.add(active);
        MUSIC_NOT_BEFORE.put(pending.context(), end);
        if (pending.context().type() != LocationType.CUSTOM) {
            PresentationHistory.record(pending.context(), Instant.now().getEpochSecond());
        }
        return active;
    }

    private static boolean allowed(LocationContext context, PresentationOverride override) {
        if (context.type() == LocationType.CUSTOM) return !override.respectClientPolicy() || ClientConfig.CUSTOM_PRESENTATIONS.get();
        ClientConfig.TitleCategory category = ClientConfig.titles(context.type());
        return PresentationPolicy.shouldShow(
            category.enabled().get(), category.showMode().get(), category.cooldownSeconds().get(),
            PresentationHistory.get(context), Instant.now().getEpochSecond());
    }

    private static PresentationDefinition definition(LocationContext context) {
        return ClientResourceIndex.definition(context).orElseGet(() -> new PresentationDefinition(
            new TitleDefinition(VisualDefinition.text(), null, null, null, null, 80, 10, 20, context.type().defaultTitlePriority()),
            null, null));
    }

    private static Component title(LocationContext context, TitleDefinition definition) {
        String registryKey = LocationNames.translationKey(context);
        if (I18n.exists(registryKey)) return Component.translatable(registryKey);
        if (definition.translationKey() != null) return Component.translatable(definition.translationKey());
        if (definition.text() != null) return Component.literal(definition.text());
        return Component.literal(LocationNames.parsedPath(context.id().getPath()));
    }

    @Nullable
    private static Component subtitle(TitleDefinition definition) {
        if (definition.subtitleTranslationKey() != null) return Component.translatable(definition.subtitleTranslationKey());
        return definition.subtitle() == null ? null : Component.literal(definition.subtitle());
    }

    private static void playEntrySound(EntrySoundDefinition sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
            SoundEvent.createVariableRangeEvent(sound.id()), sound.pitch(), sound.volume()));
    }

    public static long musicNotBefore(LocationContext context) {
        Active showing = ACTIVE.stream().filter(active -> active.context().equals(context)).findFirst().orElse(null);
        if (showing != null) return showing.endTick();
        if (QUEUE.stream().anyMatch(pending -> pending.context().equals(context))) return Long.MAX_VALUE;
        return MUSIC_NOT_BEFORE.getOrDefault(context, clientTick);
    }

    public static long clientTick() {
        return clientTick;
    }

    public static List<Active> active() {
        return List.copyOf(ACTIVE);
    }

    public static void clear() {
        QUEUE.clear();
        DEDUPLICATION.clear();
        MUSIC_NOT_BEFORE.clear();
        ACTIVE.clear();
        PresentationHistory.saveNow();
    }

    private record Pending(LocationContext context, PresentationOverride override, int priority, long enqueuedTick) { }

    public record Active(
        LocationContext context,
        PresentationDefinition definition,
        PresentationOverride override,
        Component title,
        @Nullable Component subtitle,
        long startTick,
        long endTick
    ) {
        public float alpha(float partialTick) {
            float elapsed = ClientPresentationManager.clientTick - this.startTick + partialTick;
            float remaining = this.endTick - ClientPresentationManager.clientTick - partialTick;
            int fadeIn = this.definition.title().fadeInTicks();
            int fadeOut = this.definition.title().fadeOutTicks();
            float in = fadeIn == 0 ? 1.0F : elapsed / fadeIn;
            float out = fadeOut == 0 ? 1.0F : remaining / fadeOut;
            return Math.clamp(Math.min(in, out), 0.0F, 1.0F);
        }

        public int elapsedTicks() {
            return (int) Math.max(0, ClientPresentationManager.clientTick - this.startTick);
        }
    }
}
