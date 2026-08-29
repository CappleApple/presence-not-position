package com.cappleapple.presencenotposition.integration;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** Suppresses automatic titles for temporary instances without linking to the optional mod. */
public final class InstancedNotInfiniteCompatibility {
    private static final String MOD_ID = "instancednotinfinite";
    private static final String INSTANCE_PATH_PREFIX = "instances/";

    private InstancedNotInfiniteCompatibility() {
    }

    public static boolean suppressAutomaticTitle(ResourceLocation currentDimension, LocationContext context) {
        return suppressAutomaticTitle(currentDimension, context, ModList.get().isLoaded(MOD_ID));
    }

    static boolean suppressAutomaticTitle(ResourceLocation currentDimension, LocationContext context, boolean modLoaded) {
        // Matches Instanced Not Infinite's InstanceDimensionIds.isTemporaryInstance contract.
        // Structure presentations remain automatic inside instances; custom presentations use an explicit path.
        return modLoaded
            && isTemporaryInstance(currentDimension)
            && context.type() != LocationType.STRUCTURE
            && context.type() != LocationType.CUSTOM;
    }

    private static boolean isTemporaryInstance(ResourceLocation dimension) {
        return dimension.getNamespace().equals(MOD_ID)
            && dimension.getPath().startsWith(INSTANCE_PATH_PREFIX)
            && dimension.getPath().length() > INSTANCE_PATH_PREFIX.length();
    }
}
