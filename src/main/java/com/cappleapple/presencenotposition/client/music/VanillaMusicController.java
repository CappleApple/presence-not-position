package com.cappleapple.presencenotposition.client.music;

import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.mixin.SoundEngineAccessor;
import com.cappleapple.presencenotposition.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public final class VanillaMusicController {
    private static float currentScale = 1.0F;

    private VanillaMusicController() {
    }

    public static void tick(boolean locationContextActive) {
        float target = 1.0F;
        if (locationContextActive) {
            target = switch (ClientConfig.VANILLA_MUSIC_BEHAVIOR.get()) {
                case REPLACE -> 0.0F;
                case DUCK -> ClientConfig.VANILLA_MUSIC_DUCK_VOLUME.get().floatValue();
                case ALLOW -> 1.0F;
            };
        }
        currentScale += Math.clamp(target - currentScale, -0.05F, 0.05F);
        Minecraft minecraft = Minecraft.getInstance();
        var soundEngine = ((SoundManagerAccessor) minecraft.getSoundManager()).presencenotposition$getSoundEngine();
        float categoryVolume = minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
        ((SoundEngineAccessor) soundEngine).presencenotposition$getInstanceToChannel().forEach((instance, handle) -> {
            if (instance.getSource() == SoundSource.MUSIC && !(instance instanceof DynamicMusicSound)) {
                float volume = Mth.clamp(instance.getVolume() * categoryVolume * currentScale, 0.0F, 1.0F);
                handle.execute(channel -> channel.setVolume(volume));
            }
        });
    }
}
