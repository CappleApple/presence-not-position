package com.cappleapple.presencenotposition.detection;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record LocationSample(ResourceLocation dimension, ResourceLocation biome, Set<ResourceLocation> structures, @Nullable BlockPos home) {
    public LocationSample {
        structures = Set.copyOf(structures);
        if (home != null) home = home.immutable();
    }

    public LocationSample(ResourceLocation dimension, ResourceLocation biome, Set<ResourceLocation> structures) {
        this(dimension, biome, structures, null);
    }
}
