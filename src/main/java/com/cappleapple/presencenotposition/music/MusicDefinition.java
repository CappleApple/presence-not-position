package com.cappleapple.presencenotposition.music;

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
    int priority
) {
    public MusicDefinition {
        volume = Math.max(0.0F, volume);
        startDelaySeconds = Math.max(0.0, startDelaySeconds);
        fadeInSeconds = Math.max(0.0, fadeInSeconds);
        fadeOutSeconds = Math.max(0.0, fadeOutSeconds);
        transitionFadeInSeconds = Math.max(0.0, transitionFadeInSeconds);
        transitionFadeOutSeconds = Math.max(0.0, transitionFadeOutSeconds);
        if (transitionDelaySeconds != null) transitionDelaySeconds = Math.max(0.0, transitionDelaySeconds);
    }

    public boolean hasFolder() {
        return this.folder != null || this.dayFolder != null || this.nightFolder != null;
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
