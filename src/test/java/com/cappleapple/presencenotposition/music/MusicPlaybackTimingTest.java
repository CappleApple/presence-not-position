package com.cappleapple.presencenotposition.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MusicPlaybackTimingTest {
    @Test
    void noCooldownBeforeTheFirstTrack() {
        assertTrue(new MusicPlaybackTiming().allowsPlayback(0, false));
    }

    @Test
    void zeroAddsNoDelayOrGlobalRestriction() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        assertEquals(140L, timing.afterPlaybackEnd(100, 40, 0));
        assertTrue(timing.allowsPlayback(100, false));
    }

    @Test
    void forcedCooldownAddsToResourcePackDelay() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        assertEquals(200L, timing.afterPlaybackEnd(100, 40, 3));
        assertFalse(timing.allowsPlayback(199, false));
        assertTrue(timing.allowsPlayback(200, false));
    }

    @Test
    void fadeOutMustFinishBeforeTheCooldownStarts() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        assertFalse(timing.allowsPlayback(100, true));
        timing.afterPlaybackEnd(140, 0, 3);
        assertFalse(timing.allowsPlayback(199, false));
        assertTrue(timing.allowsPlayback(200, false));
    }

    @Test
    void skipsOrShorterOutgoingCooldownsDoNotShortenTheActiveGap() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        timing.afterPlaybackEnd(100, 40, 3);
        timing.afterPlaybackEnd(110, 0, 0);
        timing.afterPlaybackEnd(120, 0, 1);
        assertFalse(timing.allowsPlayback(199, false));
        assertTrue(timing.allowsPlayback(200, false));
    }

    @Test
    void cooldownUsesLongArithmeticForTheFullConfigRange() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        long expected = 100L + Integer.MAX_VALUE + Integer.MAX_VALUE * 20L;
        assertEquals(expected, timing.afterPlaybackEnd(100, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertFalse(timing.allowsPlayback(expected - 1, false));
        assertTrue(timing.allowsPlayback(expected, false));
    }

    @Test
    void logoutClearsThePreviousSessionsCooldown() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        timing.afterPlaybackEnd(100, 40, 3);
        timing.clear();
        assertTrue(timing.allowsPlayback(0, false));
    }

    @Test
    void queuedTitlesRemainPendingInsteadOfOverflowingTheirDeadline() {
        assertEquals(Long.MAX_VALUE, MusicPlaybackTiming.afterTitle(100, Long.MAX_VALUE, 40));
        assertEquals(240L, MusicPlaybackTiming.afterTitle(100, 200, 40));
        assertEquals(140L, MusicPlaybackTiming.afterTitle(100, 20, 40));
    }

    @Test
    void cooldownDeadlineSaturatesInsteadOfWrapping() {
        MusicPlaybackTiming timing = new MusicPlaybackTiming();
        assertEquals(Long.MAX_VALUE, timing.afterPlaybackEnd(Long.MAX_VALUE - 1, 40, 3));
        assertFalse(timing.allowsPlayback(Long.MAX_VALUE - 1, false));
    }
}
