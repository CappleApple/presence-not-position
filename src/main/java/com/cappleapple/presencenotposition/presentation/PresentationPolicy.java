package com.cappleapple.presencenotposition.presentation;

import javax.annotation.Nullable;

public final class PresentationPolicy {
    private PresentationPolicy() {
    }

    public static boolean shouldShow(boolean enabled, ShowMode mode, long cooldownSeconds, @Nullable HistoryEntry history, long nowSeconds) {
        if (!enabled) return false;
        return switch (mode) {
            case ALWAYS -> true;
            case ONCE -> history == null || history.timesShown() == 0;
            case COOLDOWN -> history == null || nowSeconds - history.lastShown() >= Math.max(0L, cooldownSeconds);
        };
    }
}
