package com.cappleapple.presencenotposition.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.music.MusicSelection;
import com.cappleapple.presencenotposition.presentation.VisualDefinition;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DefinitionParserTest {
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
}
