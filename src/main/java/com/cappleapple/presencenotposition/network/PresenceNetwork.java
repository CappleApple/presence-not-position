package com.cappleapple.presencenotposition.network;

import com.cappleapple.presencenotposition.client.ClientPayloadHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class PresenceNetwork {
    private PresenceNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PresenceNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
            .playToClient(ContextPayload.TYPE, ContextPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)))
            .playToClient(PresentationPayload.TYPE, PresentationPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
    }
}
