package com.cappleapple.presencenotposition.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MusicContextResolverTest {
    private static final LocationContext DIMENSION = context(LocationType.DIMENSION, "the_nether");
    private static final LocationContext BIOME = context(LocationType.BIOME, "soul_sand_valley");
    private static final LocationContext STRUCTURE = context(LocationType.STRUCTURE, "fortress");
    private static final ResourceLocation DAY = track("day");
    private static final ResourceLocation NIGHT = track("night");

    @Test void structureBeatsBiomeAndDimensionThenBiomeIsRevealed() {
        Map<LocationContext, ResolvedMusic> definitions = Map.of(
            DIMENSION, music(track("dimension")), BIOME, music(track("biome")), STRUCTURE, music(track("structure")));
        assertEquals(STRUCTURE, resolve(Set.of(DIMENSION, BIOME, STRUCTURE), definitions, context -> true).context());
        assertEquals(BIOME, resolve(Set.of(DIMENSION, BIOME), definitions, context -> true).context());
    }

    @Test void disabledBiomeFallsThroughToDimension() {
        Map<LocationContext, ResolvedMusic> definitions = Map.of(DIMENSION, music(track("dimension")), BIOME, music(track("biome")));
        assertEquals(DIMENSION, resolve(Set.of(DIMENSION, BIOME), definitions, context -> context.type() != LocationType.BIOME).context());
    }

    @Test void missingStructureMusicDoesNotSuppressBiome() {
        Map<LocationContext, ResolvedMusic> definitions = Map.of(
            STRUCTURE, new ResolvedMusic(definition(false), new MusicTrackSet(List.of(), List.of(), List.of())),
            BIOME, music(track("biome")));
        assertEquals(BIOME, resolve(Set.of(STRUCTURE, BIOME), definitions, context -> true).context());
    }

    @Test void explicitSilenceSuppressesLowerPriority() {
        Map<LocationContext, ResolvedMusic> definitions = Map.of(
            STRUCTURE, new ResolvedMusic(definition(true), new MusicTrackSet(List.of(), List.of(), List.of())),
            BIOME, music(track("biome")));
        MusicChoice choice = resolve(Set.of(STRUCTURE, BIOME), definitions, context -> true);
        assertEquals(STRUCTURE, choice.context());
        assertTrue(choice.silence());
    }

    @Test void dayAndNightUseSpecificFoldersWithGenericFallback() {
        ResolvedMusic resolved = new ResolvedMusic(definition(false), new MusicTrackSet(List.of(track("generic")), List.of(DAY), List.of(NIGHT)));
        assertEquals(List.of(DAY), resolved.tracks().resolve(DayPeriod.DAY));
        assertEquals(List.of(NIGHT), resolved.tracks().resolve(DayPeriod.NIGHT));
        ResolvedMusic fallback = new ResolvedMusic(definition(false), new MusicTrackSet(List.of(track("generic")), List.of(), List.of()));
        assertEquals(fallback.tracks().generic(), fallback.tracks().resolve(DayPeriod.NIGHT));
    }

    private static MusicChoice resolve(Set<LocationContext> active, Map<LocationContext, ResolvedMusic> defs, java.util.function.Predicate<LocationContext> enabled) {
        return MusicContextResolver.resolve(active, defs, enabled, DayPeriod.DAY).orElseThrow();
    }

    private static ResolvedMusic music(ResourceLocation track) {
        return new ResolvedMusic(definition(false), new MusicTrackSet(List.of(track), List.of(), List.of()));
    }

    private static MusicDefinition definition(boolean silence) {
        return new MusicDefinition(null, null, null, 1, false, -16, MusicSelection.SHUFFLE, true, false, 0,
            new TrackDelay(0, 0), 1, 1, 1, 1, null, true, silence, 0);
    }

    private static LocationContext context(LocationType type, String path) { return new LocationContext(type, ResourceLocation.withDefaultNamespace(path)); }
    private static ResourceLocation track(String path) { return ResourceLocation.fromNamespaceAndPath("test", "music/" + path); }
}
