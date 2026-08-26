package com.cappleapple.presencenotposition.client;

import com.cappleapple.presencenotposition.client.music.ClientMusicManager;
import com.cappleapple.presencenotposition.network.ContextPayload;
import com.cappleapple.presencenotposition.network.PresentationPayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(ContextPayload payload) {
        ClientMusicManager.receive(payload);
    }

    public static void handle(PresentationPayload payload) {
        ClientPresentationManager.receive(payload);
    }
}
