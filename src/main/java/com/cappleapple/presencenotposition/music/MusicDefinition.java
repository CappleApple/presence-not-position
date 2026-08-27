package com.cappleapple.presencenotposition.music;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record MusicDefinition(
    @Nullable ResourceLocation folder,
    @Nullable ResourceLocation dayFolder,
    @Nullable ResourceLocation nightFolder,
    float volume,
    boolean normalizeVolume,
    double normalizationTarget,
    MusicSelection selection,
    boolean avoidImmediateRepeat,
    boolean startAfterTitle,
    double startDelaySeconds,
    TrackDelay trackDelay,
    double fadeInSeconds,
    double fadeOutSeconds,
    double transitionFadeInSeconds,
    double transitionFadeOutSeconds,
    @Nullable Double transitionDelaySeconds,
    boolean resume,
    boolean silenceLowerPriority,
    int priority,
    List<ResourceLocation> folders,
    List<ResourceLocation> dayFolders,
    List<ResourceLocation> nightFolders
) {
    public MusicDefinition {
        folders = includeLegacy(folder, folders);
        dayFolders = includeLegacy(dayFolder, dayFolders);
        nightFolders = includeLegacy(nightFolder, nightFolders);
        folder = folders.isEmpty() ? null : folders.getFirst();
        dayFolder = dayFolders.isEmpty() ? null : dayFolders.getFirst();
        nightFolder = nightFolders.isEmpty() ? null : nightFolders.getFirst();
        volume = Math.max(0.0F, volume);
        startDelaySeconds = Math.max(0.0, startDelaySeconds);
        fadeInSeconds = Math.max(0.0, fadeInSeconds);
        fadeOutSeconds = Math.max(0.0, fadeOutSeconds);
        transitionFadeInSeconds = Math.max(0.0, transitionFadeInSeconds);
        transitionFadeOutSeconds = Math.max(0.0, transitionFadeOutSeconds);
        if (transitionDelaySeconds != null) transitionDelaySeconds = Math.max(0.0, transitionDelaySeconds);
    }

    /** Retains the original constructor and single-path accessors for integrations. */
    public MusicDefinition(
        @Nullable ResourceLocation folder, @Nullable ResourceLocation dayFolder, @Nullable ResourceLocation nightFolder,
        float volume, boolean normalizeVolume, double normalizationTarget, MusicSelection selection,
        boolean avoidImmediateRepeat, boolean startAfterTitle, double startDelaySeconds, TrackDelay trackDelay,
        double fadeInSeconds, double fadeOutSeconds, double transitionFadeInSeconds, double transitionFadeOutSeconds,
        @Nullable Double transitionDelaySeconds, boolean resume, boolean silenceLowerPriority, int priority
    ) {
        this(folder, dayFolder, nightFolder, volume, normalizeVolume, normalizationTarget, selection,
            avoidImmediateRepeat, startAfterTitle, startDelaySeconds, trackDelay, fadeInSeconds, fadeOutSeconds,
            transitionFadeInSeconds, transitionFadeOutSeconds, transitionDelaySeconds, resume, silenceLowerPriority,
            priority, List.of(), List.of(), List.of());
    }

    private static List<ResourceLocation> includeLegacy(@Nullable ResourceLocation first, List<ResourceLocation> paths) {
        return Stream.concat(Stream.ofNullable(first), List.copyOf(paths).stream()).distinct().toList();
    }

    public boolean hasFolder() {
        return !this.folders.isEmpty() || !this.dayFolders.isEmpty() || !this.nightFolders.isEmpty();
    }

    public int transitionDelayTicks(com.cappleapple.presencenotposition.location.LocationType type) {
        if (this.transitionDelaySeconds != null) return secondsToTicks(this.transitionDelaySeconds);
        return switch (type) {
            case STRUCTURE, HOME -> 10;
            case BIOME -> 40;
            case DIMENSION, CUSTOM -> 0;
        };
    }

    public static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(seconds * 20.0));
    }
}
