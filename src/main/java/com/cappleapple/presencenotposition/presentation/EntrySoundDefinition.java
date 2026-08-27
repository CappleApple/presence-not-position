package com.cappleapple.presencenotposition.presentation;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

public record EntrySoundDefinition(ResourceLocation id, float volume, float pitch, List<ResourceLocation> ids) {
    public EntrySoundDefinition {
        ids = Stream.concat(Stream.ofNullable(id), List.copyOf(ids).stream()).distinct().toList();
        if (ids.isEmpty()) throw new IllegalArgumentException("entrySound requires at least one sound ID");
        id = ids.getFirst();
        volume = Math.max(0.0F, volume);
        pitch = Math.max(0.01F, pitch);
    }

    public EntrySoundDefinition(ResourceLocation id, float volume, float pitch) {
        this(id, volume, pitch, List.of());
    }
}
