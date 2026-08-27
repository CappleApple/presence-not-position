package com.cappleapple.presencenotposition.integration;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import net.neoforged.fml.ModList;

/** Suppresses automatic titles for temporary instances without linking to the optional mod. */
public final class InstancedNotInfiniteCompatibility {
    private static final String MOD_ID = "instancednotinfinite";
    private static final String INSTANCE_PATH_PREFIX = "instances/";

    private InstancedNotInfiniteCompatibility() {
    }

    public static boolean suppressAutomaticTitle(LocationContext context) {
        return suppressAutomaticTitle(context, ModList.get().isLoaded(MOD_ID));
    }

    static boolean suppressAutomaticTitle(LocationContext context, boolean modLoaded) {
        // Matches Instanced Not Infinite's InstanceDimensionIds.isTemporaryInstance contract.
        // Its other dimensions, and every non-dimension presentation, retain the normal path.
        return modLoaded
            && context.type() == LocationType.DIMENSION
            && context.id().getNamespace().equals(MOD_ID)
            && context.id().getPath().startsWith(INSTANCE_PATH_PREFIX)
            && context.id().getPath().length() > INSTANCE_PATH_PREFIX.length();
    }
}
