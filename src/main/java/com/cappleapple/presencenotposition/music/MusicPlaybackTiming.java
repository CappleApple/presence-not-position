package com.cappleapple.presencenotposition.music;

/** A client-session-wide deadline so context changes cannot bypass a forced inter-track gap. */
public final class MusicPlaybackTiming {
    private long forcedCooldownUntil = Long.MIN_VALUE;

    public long afterPlaybackEnd(long tick, int trackDelayTicks, int forcedCooldownSeconds) {
        long next = addDelay(tick, Math.max(0L, trackDelayTicks) + Math.max(0L, forcedCooldownSeconds) * 20L);
        if (forcedCooldownSeconds > 0) this.forcedCooldownUntil = Math.max(this.forcedCooldownUntil, next);
        return next;
    }

    public boolean allowsPlayback(long tick, boolean waitingForFadeOut) {
        return !waitingForFadeOut && tick >= this.forcedCooldownUntil;
    }

    public void clear() {
        this.forcedCooldownUntil = Long.MIN_VALUE;
    }

    public static long afterTitle(long tick, long titleEnd, int startDelayTicks) {
        return addDelay(Math.max(tick, titleEnd), Math.max(0L, startDelayTicks));
    }

    private static long addDelay(long tick, long delay) {
        return tick > Long.MAX_VALUE - delay ? Long.MAX_VALUE : tick + delay;
    }
}
