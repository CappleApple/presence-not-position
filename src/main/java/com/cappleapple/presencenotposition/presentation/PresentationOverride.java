package com.cappleapple.presencenotposition.presentation;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record PresentationOverride(
    @Nullable Component title,
    @Nullable Component subtitle,
    @Nullable ResourceLocation sound,
    @Nullable Integer priority,
    @Nullable Integer durationTicks,
    boolean respectClientPolicy
) {
    public static final PresentationOverride NONE = new PresentationOverride(null, null, null, null, null, true);
}
