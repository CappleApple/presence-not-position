package com.cappleapple.presencenotposition.location;

import java.util.Locale;

public enum LocationType {
    STRUCTURE("structures", 200, 3),
    BIOME("biomes", 50, 2),
    DIMENSION("dimensions", 100, 1),
    CUSTOM("custom", 300, 0),
    HOME("home", 250, 4);

    private final String resourceDirectory;
    private final int defaultTitlePriority;
    private final int musicPriority;

    LocationType(String resourceDirectory, int defaultTitlePriority, int musicPriority) {
        this.resourceDirectory = resourceDirectory;
        this.defaultTitlePriority = defaultTitlePriority;
        this.musicPriority = musicPriority;
    }

    public String resourceDirectory() {
        return this.resourceDirectory;
    }

    public int defaultTitlePriority() {
        return this.defaultTitlePriority;
    }

    public int musicPriority() {
        return this.musicPriority;
    }

    public static LocationType parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith("S")) normalized = normalized.substring(0, normalized.length() - 1);
        return valueOf(normalized);
    }
}
