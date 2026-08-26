package com.cappleapple.presencenotposition.music;

import java.util.random.RandomGenerator;

public record TrackDelay(double minSeconds, double maxSeconds) {
    public TrackDelay {
        minSeconds = Math.max(0.0, minSeconds);
        maxSeconds = Math.max(minSeconds, maxSeconds);
    }

    public int randomTicks(RandomGenerator random) {
        double seconds = this.minSeconds == this.maxSeconds
            ? this.minSeconds
            : random.nextDouble(this.minSeconds, Math.nextUp(this.maxSeconds));
        return Math.max(0, (int) Math.round(seconds * 20.0));
    }
}
