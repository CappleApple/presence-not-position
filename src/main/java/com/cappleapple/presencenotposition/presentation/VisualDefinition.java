package com.cappleapple.presencenotposition.presentation;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record VisualDefinition(Type type, @Nullable ResourceLocation texture, List<ResourceLocation> frames, AnimationDefinition animation) {
    public enum Type { TEXT, TEXTURE, FRAMES }

    public VisualDefinition {
        frames = List.copyOf(frames);
        if (type == Type.TEXTURE && texture == null) throw new IllegalArgumentException("texture visual requires texture");
        if (type == Type.FRAMES && frames.isEmpty()) throw new IllegalArgumentException("frames visual requires frames");
    }

    public static VisualDefinition text() {
        return new VisualDefinition(Type.TEXT, null, List.of(), new AnimationDefinition(1, 1, false, AnimationDefinition.Layout.AUTO));
    }
}
