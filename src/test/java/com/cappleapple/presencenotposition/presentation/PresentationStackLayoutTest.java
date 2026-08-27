package com.cappleapple.presencenotposition.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.presencenotposition.location.LocationType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresentationStackLayoutTest {
    @Test
    void simultaneousLocationsSortInVisualHierarchy() {
        List<LocationType> types = new ArrayList<>(List.of(
            LocationType.HOME, LocationType.CUSTOM, LocationType.STRUCTURE, LocationType.DIMENSION, LocationType.BIOME));

        types.sort((left, right) -> Integer.compare(
            PresentationStackLayout.order(left), PresentationStackLayout.order(right)));

        assertEquals(List.of(LocationType.DIMENSION, LocationType.BIOME, LocationType.STRUCTURE, LocationType.HOME, LocationType.CUSTOM), types);
    }

    @Test
    void highestAvailableEntryIsAlwaysLargest() {
        assertEquals(1.0F, PresentationStackLayout.scaleForIndex(0));
        assertTrue(PresentationStackLayout.scaleForIndex(0) > PresentationStackLayout.scaleForIndex(1));
        assertTrue(PresentationStackLayout.scaleForIndex(1) > PresentationStackLayout.scaleForIndex(2));
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.scaleForIndex(-1));
    }

    @Test
    void defaultStackStartsAtTheTopAndRemainsCompact() {
        for (int screenHeight : List.of(90, 240, 480, 1080)) {
            for (int count = 1; count <= 12; count++) {
                List<PresentationStackLayout.Row> rows = PresentationStackLayout.rows(screenHeight, count);
                assertEquals(count, rows.size());
                assertEquals(0, rows.get(0).top());
                assertTrue(rows.get(rows.size() - 1).bottom() - rows.get(0).top() <= screenHeight / 3,
                    "stack exceeded compact height for height=" + screenHeight + " count=" + count);
                for (int index = 1; index < rows.size(); index++) {
                    assertTrue(rows.get(index - 1).center() < rows.get(index).center());
                    assertTrue(rows.get(index - 1).height() >= rows.get(index).height());
                }
            }
        }
    }

    @Test
    void configuredTopOffsetAndSpacingAreAppliedToTheWholeStack() {
        List<PresentationStackLayout.Row> rows = PresentationStackLayout.rows(240, 3, 42, 7);

        assertEquals(42, rows.get(0).top());
        assertEquals(7, rows.get(1).top() - rows.get(0).bottom());
        assertEquals(7, rows.get(2).top() - rows.get(1).bottom());
    }

    @Test
    void horizontalAnchorAppliesSignedOffsetFromCenter() {
        assertEquals(160, PresentationStackLayout.horizontalAnchor(320, 0));
        assertEquals(187, PresentationStackLayout.horizontalAnchor(320, 27));
        assertEquals(133, PresentationStackLayout.horizontalAnchor(320, -27));
    }

    @Test
    void rejectsInvalidLayoutInputs() {
        assertTrue(PresentationStackLayout.rows(240, 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.rows(0, 1));
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.rows(240, -1));
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.rows(240, 1, 2, -1));
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.horizontalAnchor(0, 0));
    }
}
