package com.cappleapple.presencenotposition.presentation;

import net.minecraft.resources.ResourceLocation;

public record EntrySoundDefinition(ResourceLocation id, float volume, float pitch) {
    public EntrySoundDefinition {
        volume = Math.max(0.0F, volume);
        pitch = Math.max(0.01F, pitch);
    }
}
