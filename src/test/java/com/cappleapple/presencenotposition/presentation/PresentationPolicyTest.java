package com.cappleapple.presencenotposition.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PresentationPolicyTest {
    private static final HistoryEntry SHOWN = new HistoryEntry(100, 200, 1);

    @Test void disabledAlwaysWins() { assertFalse(PresentationPolicy.shouldShow(false, ShowMode.ALWAYS, 0, null, 300)); }
    @Test void alwaysIgnoresHistory() { assertTrue(PresentationPolicy.shouldShow(true, ShowMode.ALWAYS, 0, SHOWN, 201)); }
    @Test void onceIsPerMissingHistory() {
        assertTrue(PresentationPolicy.shouldShow(true, ShowMode.ONCE, 0, null, 201));
        assertFalse(PresentationPolicy.shouldShow(true, ShowMode.ONCE, 0, SHOWN, 201));
    }
    @Test void cooldownUsesLastShownForThatEntry() {
        assertFalse(PresentationPolicy.shouldShow(true, ShowMode.COOLDOWN, 100, SHOWN, 299));
        assertTrue(PresentationPolicy.shouldShow(true, ShowMode.COOLDOWN, 100, SHOWN, 300));
    }
}
