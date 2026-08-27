package com.cappleapple.presencenotposition.gametest;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.detection.LocationSample;
import com.cappleapple.presencenotposition.detection.PlayerLocationState;
import com.cappleapple.presencenotposition.integration.InstancedNotInfiniteCompatibility;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.music.DayPeriod;
import com.cappleapple.presencenotposition.music.MusicContextResolver;
import com.cappleapple.presencenotposition.music.MusicDefinition;
import com.cappleapple.presencenotposition.music.MusicSelection;
import com.cappleapple.presencenotposition.music.MusicTrackSet;
import com.cappleapple.presencenotposition.music.ResolvedMusic;
import com.cappleapple.presencenotposition.music.TrackDelay;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PresenceNotPosition.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PresenceGameTests {
    private static final String EMPTY = "empty";

    private PresenceGameTests() { }

    @GameTest(template = EMPTY)
    public static void overlappingStructuresRemainIndependent(GameTestHelper helper) {
        PlayerLocationState state = new PlayerLocationState(10, 20);
        ResourceLocation dimension = id("overworld");
        ResourceLocation biome = id("plains");
        ResourceLocation one = id("village_plains");
        ResourceLocation two = ResourceLocation.fromNamespaceAndPath("test", "arena");
        state.sample(new LocationSample(dimension, biome, Set.of(one, two)), 0);
        state.sample(new LocationSample(dimension, biome, Set.of(one)), 10);
        helper.assertTrue(state.structures().equals(Set.of(one)), "Only the independently exited structure should be removed");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void biomeBorderRequiresStableObservation(GameTestHelper helper) {
        PlayerLocationState state = new PlayerLocationState(15, 20);
        state.sample(new LocationSample(id("overworld"), id("plains"), Set.of()), 0);
        helper.assertTrue(state.sample(new LocationSample(id("overworld"), id("desert"), Set.of()), 5).isEmpty(), "Early biome sample must not transition");
        helper.assertTrue(state.sample(new LocationSample(id("overworld"), id("desert"), Set.of()), 25).size() == 2, "Stable biome must emit one exit and one enter");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dimensionChangeRebuildsContext(GameTestHelper helper) {
        PlayerLocationState state = new PlayerLocationState(15, 20);
        state.sample(new LocationSample(id("overworld"), id("plains"), Set.of(id("village"))), 0);
        var changes = state.sample(new LocationSample(id("the_nether"), id("nether_wastes"), Set.of()), 1);
        helper.assertTrue(changes.stream().anyMatch(change -> change.entered() && change.context().type() == LocationType.DIMENSION && change.context().id().equals(id("the_nether"))), "New dimension must enter immediately");
        helper.assertTrue(state.structures().isEmpty() && state.biome().equals(id("nether_wastes")), "Lower contexts must be rebuilt");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void musicSpecificityAndFallbackAreServerLoadable(GameTestHelper helper) {
        LocationContext dimension = new LocationContext(LocationType.DIMENSION, id("the_nether"));
        LocationContext biome = new LocationContext(LocationType.BIOME, id("soul_sand_valley"));
        LocationContext structure = new LocationContext(LocationType.STRUCTURE, id("fortress"));
        var definitions = Map.of(dimension, resolved("dimension"), biome, resolved("biome"), structure, resolved("structure"));
        var winner = MusicContextResolver.resolve(Set.of(dimension, biome, structure), definitions, ignored -> true, DayPeriod.DAY).orElseThrow();
        helper.assertTrue(winner.context().equals(structure), "Structure music must beat biome and dimension music");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void instanceTitleSuppressionPreservesContextsAndMusic(GameTestHelper helper) {
        ResourceLocation instance = ResourceLocation.fromNamespaceAndPath("instancednotinfinite", "instances/0123456789abcdef0123456789abcdef");
        LocationContext dimension = new LocationContext(LocationType.DIMENSION, instance);
        LocationContext biome = new LocationContext(LocationType.BIOME, id("plains"));
        LocationContext structure = new LocationContext(LocationType.STRUCTURE, id("ancient_city"));
        PlayerLocationState state = new PlayerLocationState(15, 20);
        var changes = state.sample(new LocationSample(instance, biome.id(), Set.of(structure.id())), 0);
        helper.assertTrue(changes.size() == 3 && changes.stream().allMatch(change -> change.entered()),
            "Logging in inside an instance must still enter all three location contexts");
        helper.assertTrue(state.dimension().equals(instance), "The actual instance dimension must remain tracked");
        boolean modLoaded = ModList.get().isLoaded("instancednotinfinite");
        helper.assertTrue(InstancedNotInfiniteCompatibility.suppressAutomaticTitle(dimension) == modLoaded,
            "Instance titles must be suppressed only when Instanced Not Infinite is loaded");
        helper.assertTrue(!InstancedNotInfiniteCompatibility.suppressAutomaticTitle(biome)
            && !InstancedNotInfiniteCompatibility.suppressAutomaticTitle(structure),
            "Biome and structure titles must remain available inside instances");
        var music = MusicContextResolver.resolve(Set.of(dimension, biome, structure), Map.of(dimension, resolved("instance")),
            ignored -> true, DayPeriod.DAY).orElseThrow();
        helper.assertTrue(music.context().equals(dimension), "Suppressing the title must not remove dimension music fallback");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void leavingInstanceRestoresNormalDimensionPresentation(GameTestHelper helper) {
        ResourceLocation instance = ResourceLocation.fromNamespaceAndPath("instancednotinfinite", "instances/0123456789abcdef0123456789abcdef");
        PlayerLocationState state = new PlayerLocationState(15, 20);
        state.sample(new LocationSample(instance, id("plains"), Set.of(id("ancient_city"))), 0);
        var changes = state.sample(new LocationSample(id("overworld"), id("plains"), Set.of()), 1);
        helper.assertTrue(changes.stream().anyMatch(change -> !change.entered()
            && change.context().type() == LocationType.DIMENSION && change.context().id().equals(instance)),
            "The instance dimension must still emit an exit transition");
        var enteredDimension = changes.stream().filter(change -> change.entered()
            && change.context().type() == LocationType.DIMENSION).findFirst().orElseThrow();
        helper.assertTrue(enteredDimension.context().id().equals(id("overworld"))
            && !InstancedNotInfiniteCompatibility.suppressAutomaticTitle(enteredDimension.context()),
            "Returning to the overworld must retain its normal automatic dimension title");
        helper.succeed();
    }

    private static ResolvedMusic resolved(String track) {
        MusicDefinition definition = new MusicDefinition(null, null, null, 1, false, -16, MusicSelection.SHUFFLE, true, false, 0,
            new TrackDelay(0, 0), 1, 1, 1, 1, null, true, false, 0);
        return new ResolvedMusic(definition, new MusicTrackSet(List.of(ResourceLocation.fromNamespaceAndPath("test", track)), List.of(), List.of()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
