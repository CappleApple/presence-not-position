package com.cappleapple.presencenotposition.mixin;

import com.cappleapple.presencenotposition.client.music.ClientMusicManager;
import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.music.MusicInterruptionPolicy;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void presencenotposition$suppressVanillaStarts(Music music, CallbackInfo callback) {
        var id = music.getEvent().value().getLocation();
        // Keep MusicManager.tick and NeoForge's SelectMusicEvent running for other mods.
        if (ClientMusicManager.suppressVanillaStarts() && MusicInterruptionPolicy.isVanillaBackgroundMusic(id)
            && !ClientConfig.ADDITIONAL_PRIORITY_SOUNDS.get().contains(id.toString())) callback.cancel();
    }
}
