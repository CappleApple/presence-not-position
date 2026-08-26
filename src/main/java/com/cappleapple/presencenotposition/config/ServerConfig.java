package com.cappleapple.presencenotposition.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DETECTION_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue STRUCTURE_EXIT_GRACE_TICKS;
    public static final ModConfigSpec.IntValue BIOME_STABILITY_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("detection");
        DETECTION_INTERVAL_TICKS = builder.defineInRange("intervalTicks", 5, 1, 100);
        STRUCTURE_EXIT_GRACE_TICKS = builder.defineInRange("structureExitGraceTicks", 15, 0, 200);
        BIOME_STABILITY_TICKS = builder.defineInRange("biomeStabilityTicks", 20, 0, 200);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
