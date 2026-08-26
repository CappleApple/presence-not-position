package com.cappleapple.presencenotposition.music;

import com.cappleapple.presencenotposition.location.LocationContext;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record MusicChoice(LocationContext context, MusicDefinition definition, DayPeriod period, List<ResourceLocation> tracks) {
    public MusicChoice {
        tracks = List.copyOf(tracks);
    }

    public boolean silence() {
        return this.definition.silenceLowerPriority() && this.tracks.isEmpty();
    }
}
