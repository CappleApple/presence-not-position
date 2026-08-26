package com.cappleapple.presencenotposition.server;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.network.PresentationPayload;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PresentationService {
    private PresentationService() {
    }

    public static void show(ServerPlayer player, LocationContext context) {
        show(player, context, PresentationOverride.NONE);
    }

    public static void show(ServerPlayer player, LocationContext context, PresentationOverride override) {
        PacketDistributor.sendToPlayer(player, new PresentationPayload(context, override));
    }
}
