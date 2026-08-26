package com.cappleapple.presencenotposition.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/** A directly streamed OGG resource; no sounds.json entry is required. */
public final class DynamicMusicSound extends AbstractTickableSoundInstance {
    private final Sound directSound;

    public DynamicMusicSound(ResourceLocation track) {
        super(SoundEvent.createVariableRangeEvent(track), SoundSource.MUSIC, RandomSource.create());
        this.directSound = new Sound(track, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
        this.sound = this.directSound;
        this.attenuation = Attenuation.NONE;
        this.relative = true;
        this.volume = 0.0F;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        this.sound = this.directSound;
        WeighedSoundEvents event = new WeighedSoundEvents(this.location, null);
        event.addSound(this.directSound);
        return event;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void setDynamicVolume(float volume) {
        this.volume = Math.max(0.0F, volume);
    }
}
