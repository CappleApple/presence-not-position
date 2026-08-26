package com.cappleapple.presencenotposition.client;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.client.music.ClientMusicManager;
import com.cappleapple.presencenotposition.resource.PresentationResourceLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PresenceNotPosition.MOD_ID, dist = Dist.CLIENT)
public final class PresenceNotPositionClient {
    public PresenceNotPositionClient(IEventBus modBus, ModContainer ignored) {
        modBus.addListener(PresenceNotPositionClient::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(PresenceNotPositionClient::clientTick);
        NeoForge.EVENT_BUS.addListener(LocationTitleRenderer::render);
        NeoForge.EVENT_BUS.addListener(ClientCommands::register);
        NeoForge.EVENT_BUS.addListener(PresenceNotPositionClient::loggedOut);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PresentationResourceLoader());
    }

    private static void clientTick(ClientTickEvent.Post event) {
        ClientPresentationManager.tick();
        ClientMusicManager.tick();
    }

    private static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPresentationManager.clear();
        ClientMusicManager.clear();
    }
}
