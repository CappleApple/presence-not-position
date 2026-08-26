package com.cappleapple.presencenotposition.api;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import com.cappleapple.presencenotposition.server.PresentationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PresenceApi {
    private PresenceApi() {
    }

    public static void show(ServerPlayer player, ResourceLocation customId) {
        PresentationService.show(player, new LocationContext(LocationType.CUSTOM, customId));
    }

    public static void show(ServerPlayer player, ResourceLocation customId, PresentationOverride override) {
        PresentationService.show(player, new LocationContext(LocationType.CUSTOM, customId), override);
    }
}
