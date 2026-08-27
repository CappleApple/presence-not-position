package com.cappleapple.presencenotposition.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationType;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EntrySoundSelectorTest {
    private static final EntrySoundDefinition DIMENSION = sound("dimension");
    private static final EntrySoundDefinition BIOME = sound("biome");
    private static final EntrySoundDefinition STRUCTURE = sound("structure");

    @Test
    void visualHierarchySelectsDimensionRegardlessOfArrivalOrder() {
        List<Request> arrivals = List.of(
            new Request(LocationType.STRUCTURE, candidate(STRUCTURE)),
            new Request(LocationType.BIOME, candidate(BIOME)),
            new Request(LocationType.DIMENSION, candidate(DIMENSION)));
        var ordered = arrivals.stream()
            .sorted(Comparator.comparingInt(request -> PresentationStackLayout.order(request.type())))
            .map(Request::candidate).toList();

        assertEquals(DIMENSION, EntrySoundSelector.select(ordered).orElseThrow());
    }

    @Test
    void biomeWinsWhenDimensionHasNoSound() {
        assertEquals(BIOME, EntrySoundSelector.select(List.of(
            candidate(null), candidate(BIOME), candidate(STRUCTURE))).orElseThrow());
    }

    @Test
    void structureWinsWhenHigherRowsHaveNoSound() {
        assertEquals(STRUCTURE, EntrySoundSelector.select(List.of(
            candidate(null), candidate(null), candidate(STRUCTURE))).orElseThrow());
    }

    @Test
    void sameCategoryUsesItsFirstSoundInVisualOrder() {
        assertEquals(STRUCTURE, EntrySoundSelector.select(List.of(
            candidate(STRUCTURE), candidate(sound("second_structure")))).orElseThrow());
    }

    @Test
    void scriptOverrideOnTopRowRetainsItsVolumeAndPitch() {
        var definition = new EntrySoundDefinition(id("original"), 0.4F, 1.3F);
        var top = new EntrySoundSelector.Candidate(definition, override("scripted"));

        assertEquals(new EntrySoundDefinition(id("scripted"), 0.4F, 1.3F),
            EntrySoundSelector.select(List.of(top, candidate(BIOME))).orElseThrow());
    }

    @Test
    void scriptOverrideWithoutResourceDefinitionStillTakesPriority() {
        var top = new EntrySoundSelector.Candidate(null, override("scripted"));

        assertEquals(sound("scripted"), EntrySoundSelector.select(List.of(top, candidate(BIOME))).orElseThrow());
    }

    @Test
    void lowerScriptOverrideDoesNotStealPriority() {
        var lower = new EntrySoundSelector.Candidate(null, override("scripted"));
        assertEquals(DIMENSION, EntrySoundSelector.select(List.of(candidate(DIMENSION), lower)).orElseThrow());
    }

    @Test
    void emptyOrSilentBatchesProduceNoSound() {
        assertTrue(EntrySoundSelector.select(List.of()).isEmpty());
        assertTrue(EntrySoundSelector.select(List.of(candidate(null), candidate(null))).isEmpty());
    }

    @Test
    void separateBatchesDoNotSuppressEachOther() {
        assertEquals(DIMENSION, EntrySoundSelector.select(List.of(candidate(DIMENSION))).orElseThrow());
        assertEquals(BIOME, EntrySoundSelector.select(List.of(candidate(BIOME))).orElseThrow());
    }

    @Test
    void variantsSkipMissingSoundsAndDuplicateIdsWithoutLayering() {
        var definition = new EntrySoundDefinition(id("missing"), 0.4F, 1.3F,
            List.of(id("first"), id("second"), id("first")));
        var available = Set.of(id("first"), id("second"));
        Random pickLast = new Random(0) {
            @Override public int nextInt(int bound) {
                assertEquals(2, bound, "Only two unique available variants should be eligible");
                return bound - 1;
            }
        };
        assertEquals(new EntrySoundDefinition(id("second"), 0.4F, 1.3F),
            EntrySoundSelector.select(List.of(candidate(definition), candidate(BIOME)), available::contains, pickLast).orElseThrow());
    }

    @Test
    void whollyMissingTopRowFallsThroughToTheNextAvailableRow() {
        var requests = List.of(candidate(DIMENSION), candidate(BIOME), candidate(STRUCTURE));
        assertEquals(BIOME, EntrySoundSelector.select(requests,
            sound -> !sound.equals(DIMENSION.id()), new Random(0)).orElseThrow());
        assertTrue(EntrySoundSelector.select(requests, ignored -> false, new Random(0)).isEmpty());
    }

    @Test
    void scriptOverrideReplacesTheWholeVariantList() {
        var definition = new EntrySoundDefinition(id("first"), 0.4F, 1.3F, List.of(id("second")));
        var top = new EntrySoundSelector.Candidate(definition, override("scripted"));
        assertEquals(new EntrySoundDefinition(id("scripted"), 0.4F, 1.3F),
            EntrySoundSelector.select(List.of(top, candidate(BIOME)), ignored -> true, new Random(0)).orElseThrow());
        assertEquals(BIOME, EntrySoundSelector.select(List.of(top, candidate(BIOME)),
            sound -> !sound.equals(id("scripted")), new Random(0)).orElseThrow());
    }

    private static EntrySoundSelector.Candidate candidate(EntrySoundDefinition sound) {
        return new EntrySoundSelector.Candidate(sound, PresentationOverride.NONE);
    }

    private static PresentationOverride override(String path) {
        return new PresentationOverride(null, null, id(path), null, null, true);
    }

    private static EntrySoundDefinition sound(String path) {
        return new EntrySoundDefinition(id(path), 1.0F, 1.0F);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private record Request(LocationType type, EntrySoundSelector.Candidate candidate) { }
}
