package com.cappleapple.presencenotposition.presentation;

import com.cappleapple.presencenotposition.location.LocationType;
import java.util.ArrayList;
import java.util.List;

/** Pure layout policy for simultaneous location-title batches. */
public final class PresentationStackLayout {
    private PresentationStackLayout() {
    }

    public static int order(LocationType type) {
        return switch (type) {
            case DIMENSION -> 0;
            case BIOME -> 1;
            case STRUCTURE -> 2;
            case CUSTOM -> 3;
        };
    }

    public static float scaleForIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("stack index must be non-negative");
        return 1.0F / (1.0F + index * 0.28F);
    }

    public static List<Row> rows(int screenHeight, int count) {
        if (screenHeight < 1) throw new IllegalArgumentException("screen height must be positive");
        if (count < 0) throw new IllegalArgumentException("presentation count must be non-negative");
        if (count == 0) return List.of();

        int regionBottom = Math.max(1, screenHeight / 3);
        int topMargin = Math.max(2, Math.min(6, regionBottom / 12));
        int gap = count == 1 ? 0 : 2;
        if (topMargin + count + gap * (count - 1) > regionBottom) gap = 0;

        float weightSum = 0.0F;
        for (int index = 0; index < count; index++) weightSum += scaleForIndex(index);
        int availableHeight = Math.max(count, regionBottom - topMargin - gap * (count - 1));
        float desiredBaseHeight = Math.min(38.0F, Math.max(24.0F, screenHeight / 6.0F));
        float baseHeight = Math.min(desiredBaseHeight, availableHeight / weightSum);

        List<Row> rows = new ArrayList<>(count);
        int top = topMargin;
        for (int index = 0; index < count; index++) {
            float scale = scaleForIndex(index);
            int height = Math.max(1, (int) Math.floor(baseHeight * scale));
            rows.add(new Row(top, height, scale));
            top += height + gap;
        }
        return List.copyOf(rows);
    }

    public record Row(int top, int height, float scale) {
        public int center() {
            return this.top + this.height / 2;
        }

        public int bottom() {
            return this.top + this.height;
        }
    }
}
