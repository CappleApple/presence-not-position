package com.cappleapple.presencenotposition.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MusicNormalizationServiceTest {
    @Test void explicitLufsMetadataUsesRequestedTarget() {
        NormalizationMetadata metadata = MusicNormalizationService.parse("vendor\0PNP_LOUDNESS_LUFS=-20.0\0");
        assertEquals(-20.0, metadata.measuredLufs());
        assertTrue(metadata.gainForTarget(-16.0) > 1.5F);
    }

    @Test void replayGainMetadataDegradesToCachedGain() {
        NormalizationMetadata metadata = MusicNormalizationService.parse("REPLAYGAIN_TRACK_GAIN=-6.0 dB");
        assertEquals(-6.0, metadata.replayGainDb());
        assertEquals(0.501F, metadata.gainForTarget(-16), 0.002F);
    }

    @Test void missingMetadataUsesUnityGain() {
        assertEquals(1.0F, MusicNormalizationService.parse("nothing").gainForTarget(-16));
    }
}
