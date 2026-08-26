package com.cappleapple.presencenotposition.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationTransition;
import com.cappleapple.presencenotposition.location.LocationType;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PlayerLocationStateTest {
    private static final ResourceLocation OVERWORLD = id("minecraft", "overworld");
    private static final ResourceLocation NETHER = id("minecraft", "the_nether");
    private static final ResourceLocation PLAINS = id("minecraft", "plains");
    private static final ResourceLocation DESERT = id("minecraft", "desert");
    private static final ResourceLocation VILLAGE = id("minecraft", "village_plains");
    private static final ResourceLocation ARENA = id("cataclysm", "burning_arena");

    @Test
    void structurePieceGapDoesNotRetriggerWithinGrace() {
        PlayerLocationState state = new PlayerLocationState(15, 20);
        state.sample(sample(OVERWORLD, PLAINS), 0);
        assertEquals(List.of("+STRUCTURE minecraft:village_plains"), describe(state.sample(sample(OVERWORLD, PLAINS, VILLAGE), 5)));
        assertTrue(state.sample(sample(OVERWORLD, PLAINS), 10).isEmpty());
        assertTrue(state.sample(sample(OVERWORLD, PLAINS, VILLAGE), 15).isEmpty());
        assertTrue(state.sample(sample(OVERWORLD, PLAINS), 25).isEmpty());
        assertEquals(List.of("-STRUCTURE minecraft:village_plains"), describe(state.sample(sample(OVERWORLD, PLAINS), 30)));
        assertTrue(state.sample(sample(OVERWORLD, PLAINS), 35).isEmpty());
    }

    @Test
    void overlappingStructuresEnterAndExitIndependently() {
        PlayerLocationState state = new PlayerLocationState(10, 20);
        state.sample(sample(OVERWORLD, PLAINS), 0);
        assertEquals(Set.of(VILLAGE, ARENA), stateAfter(state, sample(OVERWORLD, PLAINS, VILLAGE, ARENA), 5));
        assertTrue(state.sample(sample(OVERWORLD, PLAINS, VILLAGE), 10).isEmpty());
        assertEquals(List.of("-STRUCTURE cataclysm:burning_arena"), describe(state.sample(sample(OVERWORLD, PLAINS, VILLAGE), 15)));
        assertEquals(Set.of(VILLAGE), state.structures());
    }

    @Test
    void biomeRequiresStabilityAndBorderJitterIsIgnored() {
        PlayerLocationState state = new PlayerLocationState(15, 20);
        state.sample(sample(OVERWORLD, PLAINS), 0);
        assertTrue(state.sample(sample(OVERWORLD, DESERT), 5).isEmpty());
        assertTrue(state.sample(sample(OVERWORLD, PLAINS), 10).isEmpty());
        assertTrue(state.sample(sample(OVERWORLD, DESERT), 15).isEmpty());
        assertTrue(state.sample(sample(OVERWORLD, DESERT), 30).isEmpty());
        assertEquals(List.of("-BIOME minecraft:plains", "+BIOME minecraft:desert"), describe(state.sample(sample(OVERWORLD, DESERT), 35)));
    }

    @Test
    void dimensionTransitionExitsOldAndRebuildsAllContextImmediately() {
        PlayerLocationState state = new PlayerLocationState(15, 20);
        state.sample(sample(OVERWORLD, PLAINS, VILLAGE), 0);
        assertEquals(List.of(
            "-STRUCTURE minecraft:village_plains",
            "-BIOME minecraft:plains",
            "-DIMENSION minecraft:overworld",
            "+DIMENSION minecraft:the_nether",
            "+BIOME minecraft:desert",
            "+STRUCTURE cataclysm:burning_arena"
        ), describe(state.sample(sample(NETHER, DESERT, ARENA), 1)));
    }

    private static Set<ResourceLocation> stateAfter(PlayerLocationState state, LocationSample sample, long tick) {
        state.sample(sample, tick);
        return state.structures();
    }

    private static List<String> describe(List<LocationTransition> transitions) {
        return transitions.stream().map(transition -> (transition.entered() ? "+" : "-")
            + transition.context().type() + " " + transition.context().id()).toList();
    }

    private static LocationSample sample(ResourceLocation dimension, ResourceLocation biome, ResourceLocation... structures) {
        return new LocationSample(dimension, biome, Set.of(structures));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
