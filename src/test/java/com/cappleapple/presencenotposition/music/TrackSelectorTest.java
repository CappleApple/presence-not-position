package com.cappleapple.presencenotposition.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class TrackSelectorTest {
    private static final List<ResourceLocation> TRACKS = List.of(id("a"), id("b"), id("c"));

    @Test void sequentialCyclesInOrder() {
        TrackSelector selector = new TrackSelector(TRACKS, MusicSelection.SEQUENTIAL, true, new Random(1));
        assertEquals(TRACKS.get(0), selector.next());
        assertEquals(TRACKS.get(1), selector.next());
        assertEquals(TRACKS.get(2), selector.next());
        assertEquals(TRACKS.get(0), selector.next());
    }

    @Test void shufflePlaysEveryTrackAndAvoidsCycleBoundaryRepeat() {
        TrackSelector selector = new TrackSelector(TRACKS, MusicSelection.SHUFFLE, true, new Random(7));
        ResourceLocation first = selector.next();
        ResourceLocation second = selector.next();
        ResourceLocation third = selector.next();
        assertEquals(Set.copyOf(TRACKS), new HashSet<>(List.of(first, second, third)));
        assertNotEquals(third, selector.next());
    }

    @Test void randomAvoidsImmediateRepeat() {
        TrackSelector selector = new TrackSelector(TRACKS, MusicSelection.RANDOM, true, new Random(2));
        ResourceLocation previous = selector.next();
        for (int index = 0; index < 50; index++) {
            ResourceLocation next = selector.next();
            assertNotEquals(previous, next);
            previous = next;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
