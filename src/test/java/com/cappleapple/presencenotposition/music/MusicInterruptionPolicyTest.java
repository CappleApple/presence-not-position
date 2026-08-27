package com.cappleapple.presencenotposition.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.client.music.DynamicMusicSound;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

class MusicInterruptionPolicyTest {
    @Test void jukeboxesAndExternalMusicTakePriorityButVanillaBackgroundAndEffectsDoNot() {
        assertTrue(priority("minecraft:music_disc.cat", SoundSource.RECORDS));
        assertTrue(priority("example:boss_theme", SoundSource.MUSIC));
        assertTrue(priority("minecraft:music.dragon", SoundSource.MUSIC));
        assertFalse(priority("minecraft:music.game", SoundSource.MUSIC));
        assertFalse(priority("minecraft:music.overworld.forest", SoundSource.MUSIC));
        assertFalse(priority("minecraft:music.nether.basalt_deltas", SoundSource.MUSIC));
        assertFalse(priority("example:boss_roar", SoundSource.HOSTILE));
        assertFalse(priority("example:wind", SoundSource.AMBIENT));
    }

    @Test void customCategoryBossMusicCanBeListedAndPnpNeverMutesItself() {
        ResourceLocation id = ResourceLocation.parse("example:boss_theme");
        assertTrue(MusicInterruptionPolicy.takesPriority(id, SoundSource.HOSTILE, false, List.of(id.toString())));
        assertFalse(MusicInterruptionPolicy.takesPriority(id, SoundSource.MUSIC, true, List.of(id.toString())));
        assertFalse(MusicInterruptionPolicy.takesPriority(id, SoundSource.MASTER, false, List.of()));
    }

    @Test void positionalRecordsOnlyInterruptInsideTheirActualAttenuationRange() {
        // Vanilla jukebox songs have volume 4 and attenuation distance 16: a 64-block range.
        assertTrue(MusicInterruptionPolicy.isAudible(false, 4, 1, 1, true, 63 * 63, 16));
        assertFalse(MusicInterruptionPolicy.isAudible(false, 4, 1, 1, true, 64 * 64, 16));
        assertFalse(MusicInterruptionPolicy.isAudible(false, 4, 1, 1, true, 100 * 100, 16));
        assertTrue(MusicInterruptionPolicy.isAudible(false, 1, 1, 1, false, 1000 * 1000, 16));
    }

    @Test void stoppedAndSilentSoundsCannotKeepLocationMusicMuted() {
        assertFalse(MusicInterruptionPolicy.isAudible(true, 1, 1, 1, false, 0, 16));
        assertFalse(MusicInterruptionPolicy.isAudible(false, 0, 1, 1, false, 0, 16));
        assertFalse(MusicInterruptionPolicy.isAudible(false, 1, 0, 1, false, 0, 16));
        assertFalse(MusicInterruptionPolicy.isAudible(false, 1, 1, 0, false, 0, 16));
    }

    @Test void mutingDoesNotLoseTheUnderlyingFadeVolumeAndCanBeReversed() {
        DynamicMusicSound sound = new DynamicMusicSound(ResourceLocation.parse("test:music/home"));
        sound.setDynamicVolume(0.8F);
        assertEquals(0.8F, sound.getVolume());
        assertTrue(sound.setMuted(true));
        assertEquals(0, sound.getVolume());
        sound.setDynamicVolume(0.4F);
        assertEquals(0, sound.getVolume());
        assertFalse(sound.setMuted(true));
        assertTrue(sound.setMuted(false));
        assertEquals(0.4F, sound.getVolume());
        assertFalse(sound.isStopped());
    }

    private static boolean priority(String id, SoundSource source) {
        return MusicInterruptionPolicy.takesPriority(ResourceLocation.parse(id), source, false, List.of());
    }
}
