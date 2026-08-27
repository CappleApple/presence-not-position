package com.cappleapple.presencenotposition.music;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Sound-category policy shared by interruption detection and vanilla-only volume control. */
public final class MusicInterruptionPolicy {
    private MusicInterruptionPolicy() { }

    public static boolean isVanillaBackgroundMusic(ResourceLocation id) {
        return id.getNamespace().equals("minecraft") && id.getPath().startsWith("music.")
            && !id.getPath().equals("music.dragon");
    }

    public static boolean takesPriority(ResourceLocation id, SoundSource source, boolean locationMusic,
                                        List<? extends String> additionalSounds) {
        if (locationMusic) return false;
        return source == SoundSource.RECORDS
            || (source == SoundSource.MUSIC && !isVanillaBackgroundMusic(id))
            || additionalSounds.contains(id.toString());
    }

    public static boolean isAudible(boolean stopped, float volume, float categoryVolume, float masterVolume,
                                    boolean attenuated, double distanceSquared, double attenuationDistance) {
        if (stopped || volume <= 0 || categoryVolume <= 0 || masterVolume <= 0) return false;
        double range = Math.max(volume, 1.0) * attenuationDistance;
        return !attenuated || distanceSquared < range * range;
    }
}
