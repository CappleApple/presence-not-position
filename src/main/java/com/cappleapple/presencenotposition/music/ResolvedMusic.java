package com.cappleapple.presencenotposition.music;

public record ResolvedMusic(MusicDefinition definition, MusicTrackSet tracks) {
    public boolean isUsable(DayPeriod period) {
        return this.definition.silenceLowerPriority() || !this.tracks.resolve(period).isEmpty();
    }
}
