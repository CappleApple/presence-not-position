package com.cappleapple.presencenotposition.music;

import javax.annotation.Nullable;

public record NormalizationMetadata(@Nullable Double measuredLufs, @Nullable Double replayGainDb) {
    public static final NormalizationMetadata UNKNOWN = new NormalizationMetadata(null, null);

    public float gainForTarget(double targetLufs) {
        double gainDb;
        if (this.measuredLufs != null) gainDb = targetLufs - this.measuredLufs;
        else if (this.replayGainDb != null) gainDb = this.replayGainDb;
        else return 1.0F;
        return (float) Math.clamp(Math.pow(10.0, gainDb / 20.0), 0.25, 4.0);
    }
}
