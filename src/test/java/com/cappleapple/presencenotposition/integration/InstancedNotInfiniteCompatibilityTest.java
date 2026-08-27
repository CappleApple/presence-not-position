package com.cappleapple.presencenotposition.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class InstancedNotInfiniteCompatibilityTest {
    private static final String INSTANCE = "instancednotinfinite:instances/0123456789abcdef0123456789abcdef";

    @Test
    void temporaryInstanceDimensionTitlesAreSuppressedWhenModIsLoaded() {
        assertTrue(suppressed(LocationType.DIMENSION, INSTANCE, true));
        assertTrue(suppressed(LocationType.DIMENSION, "instancednotinfinite:instances/another_instance", true));
    }

    @Test
    void absentModLeavesEveryPresentationUntouched() {
        for (LocationType type : LocationType.values()) {
            assertFalse(suppressed(type, INSTANCE, false));
        }
    }

    @Test
    void biomeStructureAndCustomTitlesAreNotSuppressed() {
        for (LocationType type : new LocationType[] {LocationType.BIOME, LocationType.STRUCTURE, LocationType.CUSTOM}) {
            assertFalse(suppressed(type, INSTANCE, true));
        }
        assertFalse(suppressed(LocationType.BIOME, "minecraft:plains", true));
        assertFalse(suppressed(LocationType.STRUCTURE, "minecraft:ancient_city", true));
    }

    @Test
    void vanillaAndOtherModDimensionsAreNotSuppressed() {
        for (String id : new String[] {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
            "other_mod:instances/0123456789abcdef0123456789abcdef", "other_mod:dungeon"}) {
            assertFalse(suppressed(LocationType.DIMENSION, id, true), id);
        }
    }

    @Test
    void otherInstancedNotInfiniteDimensionsAreNotSuppressed() {
        for (String path : new String[] {"lobby", "instances", "instances/", "instances_extra/example", "other/instances/example"}) {
            assertFalse(suppressed(LocationType.DIMENSION, "instancednotinfinite:" + path, true), path);
        }
    }

    private static boolean suppressed(LocationType type, String id, boolean modLoaded) {
        return InstancedNotInfiniteCompatibility.suppressAutomaticTitle(
            new LocationContext(type, ResourceLocation.parse(id)), modLoaded);
    }
}
