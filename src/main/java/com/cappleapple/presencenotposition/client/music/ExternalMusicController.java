package com.cappleapple.presencenotposition.client.music;

import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.mixin.SoundEngineAccessor;
import com.cappleapple.presencenotposition.mixin.SoundManagerAccessor;
import com.cappleapple.presencenotposition.music.MusicInterruptionPolicy;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ExternalMusicController {
    private ExternalMusicController() { }

    public static boolean isPriorityMusicPlaying(Minecraft minecraft) {
        Vec3 listener = minecraft.gameRenderer.getMainCamera().getPosition();
        var additional = ClientConfig.ADDITIONAL_PRIORITY_SOUNDS.get();
        return channels(minecraft).entrySet().stream().anyMatch(entry -> {
            SoundInstance sound = entry.getKey();
            if (!MusicInterruptionPolicy.takesPriority(sound.getLocation(), sound.getSource(),
                sound instanceof DynamicMusicSound, additional)) return false;
            double distanceSquared = sound.isRelative()
                ? new Vec3(sound.getX(), sound.getY(), sound.getZ()).lengthSqr()
                : listener.distanceToSqr(sound.getX(), sound.getY(), sound.getZ());
            return MusicInterruptionPolicy.isAudible(entry.getValue().isStopped(), sound.getVolume(),
                minecraft.options.getSoundSourceVolume(sound.getSource()),
                minecraft.options.getSoundSourceVolume(SoundSource.MASTER),
                sound.getAttenuation() == SoundInstance.Attenuation.LINEAR,
                distanceSquared, sound.getSound().getAttenuationDistance());
        });
    }

    public static void muteLocationMusic(Minecraft minecraft, boolean muted) {
        float categoryVolume = minecraft.options.getSoundSourceVolume(SoundSource.MUSIC);
        channels(minecraft).forEach((instance, handle) -> {
            if (instance instanceof DynamicMusicSound sound && sound.setMuted(muted)) {
                // Apply on the same tick, including outgoing crossfades and suspended streams.
                float volume = Mth.clamp(sound.getVolume() * categoryVolume, 0.0F, 1.0F);
                handle.execute(channel -> channel.setVolume(volume));
            }
        });
    }

    private static Map<SoundInstance, ChannelAccess.ChannelHandle> channels(Minecraft minecraft) {
        var engine = ((SoundManagerAccessor) minecraft.getSoundManager()).presencenotposition$getSoundEngine();
        return ((SoundEngineAccessor) engine).presencenotposition$getInstanceToChannel();
    }
}
