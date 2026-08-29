package com.cappleapple.presencenotposition.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class InstancedNotInfiniteCompatibilityTest {
    private static final ResourceLocation INSTANCE = id("instancednotinfinite:instances/0123456789abcdef0123456789abcdef");

    @Test
    void instanceSuppressesAutomaticDimensionBiomeAndHomePresentations() {
        assertTrue(suppressed(INSTANCE, LocationType.DIMENSION, "instancednotinfinite:instances/another_instance", true));
        assertTrue(suppressed(INSTANCE, LocationType.BIOME, "minecraft:plains", true));
        assertTrue(suppressed(INSTANCE, LocationType.HOME, "minecraft:bed", true));
    }

    @Test
    void instanceRetainsStructureAndExplicitCustomPresentations() {
        assertFalse(suppressed(INSTANCE, LocationType.STRUCTURE, "minecraft:ancient_city", true));
        assertFalse(suppressed(INSTANCE, LocationType.CUSTOM, "example:warning", true));
    }

    @Test
    void absentModLeavesEveryPresentationUntouched() {
        for (LocationType type : LocationType.values()) {
            assertFalse(suppressed(INSTANCE, type, "minecraft:test", false));
        }
    }

    @Test
    void normalDimensionsLeaveEveryPresentationUntouched() {
        for (LocationType type : LocationType.values()) {
            assertFalse(suppressed(id("minecraft:overworld"), type, "minecraft:test", true));
        }
    }

    @Test
    void onlyTemporaryInstancedNotInfiniteDimensionsActivateTheFilter() {
        for (String dimension : new String[] {"instancednotinfinite:lobby", "instancednotinfinite:instances",
            "instancednotinfinite:instances/", "instancednotinfinite:instances_extra/example",
            "other_mod:instances/0123456789abcdef0123456789abcdef"}) {
            assertFalse(suppressed(id(dimension), LocationType.BIOME, "minecraft:plains", true), dimension);
        }
    }

    private static boolean suppressed(
        ResourceLocation currentDimension,
        LocationType type,
        String contextId,
        boolean modLoaded
    ) {
        return InstancedNotInfiniteCompatibility.suppressAutomaticTitle(
            currentDimension, new LocationContext(type, id(contextId)), modLoaded);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }
}
