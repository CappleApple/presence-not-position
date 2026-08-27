package com.cappleapple.presencenotposition.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.music.MusicSelection;
import com.cappleapple.presencenotposition.music.DayPeriod;
import com.cappleapple.presencenotposition.presentation.VisualDefinition;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DefinitionParserTest {
    @Test void singularStringsArraysAndPluralNamesProduceEquivalentDefinitions() {
        var single = JsonParser.parseString("""
            {"entrySound":{"id":"test:intro"},
             "music":{"folder":"test:music/base","dayFolder":"test:music/day","nightFolder":"test:music/night"}}
            """).getAsJsonObject();
        var arrays = JsonParser.parseString("""
            {"entrySound":{"id":["test:intro"]},
             "music":{"folder":["test:music/base"],"dayFolder":["test:music/day"],"nightFolder":["test:music/night"]}}
            """).getAsJsonObject();
        var plural = JsonParser.parseString("""
            {"entrySound":{"ids":["test:intro"]},
             "music":{"folders":["test:music/base"],"dayFolders":["test:music/day"],"nightFolders":["test:music/night"]}}
            """).getAsJsonObject();
        for (LocationType type : LocationType.values()) {
            var expected = DefinitionParser.parse(type, single);
            assertEquals(expected, DefinitionParser.parse(type, arrays));
            assertEquals(expected, DefinitionParser.parse(type, plural));
        }
    }

    @Test void singularAndPluralPathsCombineInDeclaredOrderWithoutDuplicates() {
        var definition = DefinitionParser.parse(LocationType.HOME, JsonParser.parseString("""
            {"entrySound":{"id":"test:intro","ids":["other:intro","test:intro"]},
             "music":{"folder":["test:music/base","other:music/base"],
                      "folders":["other:music/base","third:music/base"]}}
            """).getAsJsonObject());
        assertEquals(List.of(ResourceLocation.parse("test:intro"), ResourceLocation.parse("other:intro")), definition.entrySound().ids());
        assertEquals(List.of(ResourceLocation.parse("test:music/base"), ResourceLocation.parse("other:music/base"),
            ResourceLocation.parse("third:music/base")), definition.music().folders());
        assertEquals(ResourceLocation.parse("test:music/base"), definition.music().folder());
        assertThrows(UnsupportedOperationException.class, () -> definition.music().folders().clear());
        assertThrows(UnsupportedOperationException.class, () -> definition.entrySound().ids().clear());
    }

    @Test void overlappingFoldersAcrossNamespacesMergeAndKeepDayNightOutOfGenericTracks() {
        var definition = DefinitionParser.parse(LocationType.HOME, JsonParser.parseString("""
            {"music":{"folders":["second:music/base","first:music/base","first:music/base/nested"],
                      "dayFolders":["first:music/base/day","second:music/base/day"],
                      "nightFolders":["first:music/base/night","second:music/base/night"]}}
            """).getAsJsonObject());
        Set<ResourceLocation> resources = Set.of(
            ResourceLocation.parse("second:music/base/b"), ResourceLocation.parse("second:music/base/a"),
            ResourceLocation.parse("first:music/base/a"), ResourceLocation.parse("first:music/base/nested/c"),
            ResourceLocation.parse("first:music/base/day/sun"), ResourceLocation.parse("second:music/base/day/sun"),
            ResourceLocation.parse("first:music/base/night/moon"), ResourceLocation.parse("second:music/base/night/moon"),
            ResourceLocation.parse("third:music/base/unlisted"), ResourceLocation.parse("first:music/base_extra/unlisted"));
        var tracks = PresentationResourceLoader.resolveMusic(Map.of(LocationContext.HOME, definition), resources)
            .get(LocationContext.HOME).tracks();
        assertEquals(List.of(ResourceLocation.parse("second:music/base/a"), ResourceLocation.parse("second:music/base/b"),
            ResourceLocation.parse("first:music/base/a"), ResourceLocation.parse("first:music/base/nested/c")), tracks.generic());
        assertEquals(List.of(ResourceLocation.parse("first:music/base/day/sun"), ResourceLocation.parse("second:music/base/day/sun")),
            tracks.resolve(DayPeriod.DAY));
        assertEquals(List.of(ResourceLocation.parse("first:music/base/night/moon"), ResourceLocation.parse("second:music/base/night/moon")),
            tracks.resolve(DayPeriod.NIGHT));
    }

    @Test void missingPathsDoNotHideOtherSourcesOrBreakPeriodFallback() {
        var definition = DefinitionParser.parse(LocationType.HOME, JsonParser.parseString("""
            {"music":{"folders":["missing:music","test:music/base"],"dayFolders":["missing:day"],"nightFolders":[]}}
            """).getAsJsonObject());
        ResourceLocation track = ResourceLocation.parse("test:music/base/a");
        var tracks = PresentationResourceLoader.resolveMusic(Map.of(LocationContext.HOME, definition), Set.of(track))
            .get(LocationContext.HOME).tracks();
        assertEquals(List.of(track), tracks.resolve(DayPeriod.DAY));
        assertEquals(List.of(track), tracks.resolve(DayPeriod.NIGHT));
        assertTrue(PresentationResourceLoader.resolveMusic(Map.of(LocationContext.HOME, definition), Set.of())
            .get(LocationContext.HOME).tracks().isEmpty());
    }

    @Test void emptyMusicListsStillAllowIntentionalSilenceButEntrySoundsRequireAnId() {
        var definition = DefinitionParser.parse(LocationType.HOME, JsonParser.parseString("""
            {"music":{"folders":[],"dayFolder":[],"nightFolders":[],"silenceLowerPriority":true}}
            """).getAsJsonObject());
        assertFalse(definition.music().hasFolder());
        assertTrue(definition.music().silenceLowerPriority());
        assertThrows(IllegalArgumentException.class, () -> DefinitionParser.parse(LocationType.HOME,
            JsonParser.parseString("{\"entrySound\":{\"ids\":[]}}").getAsJsonObject()));
    }

    @Test void malformedListEntriesAreRejectedInsteadOfSilentlyChangingResourceIds() {
        for (String value : List.of("[\"test:valid\",42]", "[null]", "[[\"test:nested\"]]", "{}", "[\"Invalid:path\"]")) {
            assertThrows(IllegalArgumentException.class, () -> DefinitionParser.parse(LocationType.HOME,
                JsonParser.parseString("{\"music\":{\"folders\":" + value + "}}").getAsJsonObject()));
            assertThrows(IllegalArgumentException.class, () -> DefinitionParser.parse(LocationType.HOME,
                JsonParser.parseString("{\"entrySound\":{\"ids\":" + value + "}}").getAsJsonObject()));
        }
    }

    @Test void completeDefinitionParsesDayNightAnimationAndDelays() {
        var definition = DefinitionParser.parse(LocationType.STRUCTURE, JsonParser.parseString("""
            {
              "title": {"visual":{"type":"texture","texture":"test:title.png","animation":{"frameCount":12,"frameTime":2,"loop":true}},"duration":100},
              "entrySound":{"id":"test:intro","volume":0.8},
              "music":{"folder":"test:music/base","dayFolder":"test:music/day","nightFolder":"test:music/night","selection":"sequential","trackDelay":{"min":3,"max":7},"resume":false}
            }
            """).getAsJsonObject());
        assertEquals(VisualDefinition.Type.TEXTURE, definition.title().visual().type());
        assertEquals(2, definition.title().visual().animation().frameAt(100));
        assertTrue(definition.title().visual().animation().loop());
        assertEquals(MusicSelection.SEQUENTIAL, definition.music().selection());
        assertEquals(3, definition.music().trackDelay().minSeconds());
        assertFalse(definition.music().resume());
    }

    @Test void fixedTrackDelayAndDefaultsParse() {
        var definition = DefinitionParser.parse(LocationType.BIOME, JsonParser.parseString("{" +
            "\"music\":{\"folder\":\"test:music\",\"trackDelay\":0}}" ).getAsJsonObject());
        assertEquals(0, definition.music().trackDelay().maxSeconds());
        assertTrue(definition.music().avoidImmediateRepeat());
    }

    @Test void resourcePathsKeepRegistryNamespaceDistinct() {
        assertEquals(
            ResourceLocation.fromNamespaceAndPath("cataclysm", "burning_arena"),
            PresentationResourceLoader.target(ResourceLocation.fromNamespaceAndPath("within", "structures/cataclysm/burning_arena/presentation")).id());
        assertEquals(
            ResourceLocation.fromNamespaceAndPath("within", "deep/dark_warning"),
            PresentationResourceLoader.target(ResourceLocation.fromNamespaceAndPath("within", "custom/deep/dark_warning/presentation")).id());
    }

    @Test void homePathUsesTheSameTitleSoundAndDayNightMusicDefinition() {
        var home = PresentationResourceLoader.target(ResourceLocation.parse("within:home/presentation"));
        assertEquals(LocationContext.HOME, home);
        var definition = DefinitionParser.parse(home.type(), JsonParser.parseString("""
            {
              "title": {"text":"Welcome Home", "subtitle":"Rest a while", "duration":100},
              "entrySound": {"id":"minecraft:block.amethyst_block.chime", "volume":0.5},
              "music": {"folder":"within:music/home", "dayFolder":"within:music/home/day",
                        "nightFolder":"within:music/home/night", "startAfterTitle":true}
            }
            """).getAsJsonObject());
        assertEquals("Welcome Home", definition.title().text());
        assertEquals(ResourceLocation.parse("minecraft:block.amethyst_block.chime"), definition.entrySound().id());
        assertEquals(ResourceLocation.parse("within:music/home/day"), definition.music().dayFolder());
        assertEquals(ResourceLocation.parse("within:music/home/night"), definition.music().nightFolder());
        assertTrue(definition.music().startAfterTitle());
    }
}
