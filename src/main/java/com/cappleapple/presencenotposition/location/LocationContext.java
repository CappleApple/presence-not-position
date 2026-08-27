package com.cappleapple.presencenotposition.location;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record LocationContext(LocationType type, ResourceLocation id) implements Comparable<LocationContext> {
    public static final LocationContext HOME = new LocationContext(LocationType.HOME,
        ResourceLocation.fromNamespaceAndPath("presencenotposition", "home"));

    public LocationContext {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }

    @Override
    public int compareTo(LocationContext other) {
        int byType = Integer.compare(other.type.musicPriority(), this.type.musicPriority());
        return byType != 0 ? byType : this.id.toString().compareTo(other.id.toString());
    }

    @Override
    public String toString() {
        return this.type + " " + this.id;
    }
}
