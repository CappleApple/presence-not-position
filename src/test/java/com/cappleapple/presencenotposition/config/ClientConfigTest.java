package com.cappleapple.presencenotposition.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationType;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

class ClientConfigTest {
    @Test
    void everyMusicCategoryHasItsOwnZeroDefaultCooldown() {
        for (String category : List.of("structures", "biomes", "dimensions")) {
            ModConfigSpec.ValueSpec spec = ClientConfig.SPEC.getSpec().get(List.of(category, "musicCooldownSeconds"));
            assertEquals(0, spec.getDefault());
            assertTrue(spec.test(0));
            assertTrue(spec.test(30));
            assertFalse(spec.test(-1));
        }
    }

    @Test
    void musicCooldownDoesNotChangeTheTitlePolicyCooldown() {
        assertEquals(300, ClientConfig.titles(LocationType.STRUCTURE).cooldownSeconds().getDefault());
        assertEquals(600, ClientConfig.titles(LocationType.BIOME).cooldownSeconds().getDefault());
        assertEquals(0, ClientConfig.titles(LocationType.DIMENSION).cooldownSeconds().getDefault());
    }
}
