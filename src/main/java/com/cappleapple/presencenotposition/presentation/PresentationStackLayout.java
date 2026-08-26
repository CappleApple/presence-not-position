package com.cappleapple.presencenotposition.presentation;

import com.cappleapple.presencenotposition.location.LocationType;
import java.util.ArrayList;
import java.util.List;

/** Pure layout policy for simultaneous location-title batches. */
public final class PresentationStackLayout {
    public static final int DEFAULT_SPACING = 2;

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
        return rows(screenHeight, count, 0, DEFAULT_SPACING);
    }

    public static List<Row> rows(int screenHeight, int count, int offsetY, int spacing) {
        if (screenHeight < 1) throw new IllegalArgumentException("screen height must be positive");
        if (count < 0) throw new IllegalArgumentException("presentation count must be non-negative");
        if (spacing < 0) throw new IllegalArgumentException("title spacing must be non-negative");
        if (count == 0) return List.of();

        int regionBottom = Math.max(1, screenHeight / 3);
        int topMargin = Math.max(2, Math.min(6, regionBottom / 12));
        int maxCompactGap = count == 1 ? 0 : Math.max(0, (regionBottom - topMargin - count) / (count - 1));
        int gap = Math.min(spacing, maxCompactGap);

        float weightSum = 0.0F;
        for (int index = 0; index < count; index++) weightSum += scaleForIndex(index);
        int availableHeight = Math.max(count, regionBottom - topMargin - gap * (count - 1));
        float desiredBaseHeight = Math.min(38.0F, Math.max(24.0F, screenHeight / 6.0F));
        float baseHeight = Math.min(desiredBaseHeight, availableHeight / weightSum);

        List<Row> rows = new ArrayList<>(count);
        long top = offsetY;
        for (int index = 0; index < count; index++) {
            float scale = scaleForIndex(index);
            int height = Math.max(1, (int) Math.floor(baseHeight * scale));
            rows.add(new Row(saturatedInt(top), height, scale));
            top += height + gap;
        }
        return List.copyOf(rows);
    }

    public static int horizontalAnchor(int screenWidth, int offsetX) {
        if (screenWidth < 1) throw new IllegalArgumentException("screen width must be positive");
        return saturatedInt((long) screenWidth / 2 + offsetX);
    }

    private static int saturatedInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value;
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
