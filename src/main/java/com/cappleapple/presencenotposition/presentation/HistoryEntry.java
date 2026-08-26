package com.cappleapple.presencenotposition.presentation;

public record HistoryEntry(long firstShown, long lastShown, int timesShown) {
    public HistoryEntry shown(long now) {
        return new HistoryEntry(this.firstShown, now, this.timesShown + 1);
    }

    public static HistoryEntry first(long now) {
        return new HistoryEntry(now, now, 1);
    }
}
