package com.cappleapple.presencenotposition.music;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record MusicTrackSet(List<ResourceLocation> generic, List<ResourceLocation> day, List<ResourceLocation> night) {
    public MusicTrackSet {
        generic = List.copyOf(generic);
        day = List.copyOf(day);
        night = List.copyOf(night);
    }

    public List<ResourceLocation> resolve(DayPeriod period) {
        List<ResourceLocation> specific = period == DayPeriod.DAY ? this.day : this.night;
        return specific.isEmpty() ? this.generic : specific;
    }

    public boolean isEmpty() {
        return this.generic.isEmpty() && this.day.isEmpty() && this.night.isEmpty();
    }
}
