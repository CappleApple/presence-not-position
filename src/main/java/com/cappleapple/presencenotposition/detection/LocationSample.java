package com.cappleapple.presencenotposition.detection;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record LocationSample(ResourceLocation dimension, ResourceLocation biome, Set<ResourceLocation> structures) {
    public LocationSample {
        structures = Set.copyOf(structures);
    }
}
