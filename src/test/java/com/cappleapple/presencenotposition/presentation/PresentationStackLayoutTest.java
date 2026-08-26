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
            LocationType.STRUCTURE, LocationType.DIMENSION, LocationType.BIOME));

        types.sort((left, right) -> Integer.compare(
            PresentationStackLayout.order(left), PresentationStackLayout.order(right)));

        assertEquals(List.of(LocationType.DIMENSION, LocationType.BIOME, LocationType.STRUCTURE), types);
    }

    @Test
    void highestAvailableEntryIsAlwaysLargest() {
        assertEquals(1.0F, PresentationStackLayout.scaleForIndex(0));
        assertTrue(PresentationStackLayout.scaleForIndex(0) > PresentationStackLayout.scaleForIndex(1));
        assertTrue(PresentationStackLayout.scaleForIndex(1) > PresentationStackLayout.scaleForIndex(2));
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.scaleForIndex(-1));
    }

    @Test
    void everyRowFitsInsideTheTopThirdAndRemainsCompact() {
        for (int screenHeight : List.of(90, 240, 480, 1080)) {
            for (int count = 1; count <= 12; count++) {
                List<PresentationStackLayout.Row> rows = PresentationStackLayout.rows(screenHeight, count);
                assertEquals(count, rows.size());
                assertTrue(rows.get(rows.size() - 1).bottom() <= screenHeight / 3,
                    "stack escaped top third for height=" + screenHeight + " count=" + count);
                for (int index = 1; index < rows.size(); index++) {
                    assertTrue(rows.get(index - 1).center() < rows.get(index).center());
                    assertTrue(rows.get(index - 1).height() >= rows.get(index).height());
                }
            }
        }
    }

    @Test
    void rejectsInvalidLayoutInputs() {
        assertTrue(PresentationStackLayout.rows(240, 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.rows(0, 1));
        assertThrows(IllegalArgumentException.class, () -> PresentationStackLayout.rows(240, -1));
    }
}
