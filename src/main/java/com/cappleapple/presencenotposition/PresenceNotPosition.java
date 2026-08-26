package com.cappleapple.presencenotposition;

import com.cappleapple.presencenotposition.config.ClientConfig;
import com.cappleapple.presencenotposition.config.ServerConfig;
import com.cappleapple.presencenotposition.network.PresenceNetwork;
import com.cappleapple.presencenotposition.server.ServerCommands;
import com.cappleapple.presencenotposition.server.ServerLocationTracker;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(PresenceNotPosition.MOD_ID)
public final class PresenceNotPosition {
    public static final String MOD_ID = "presencenotposition";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PresenceNotPosition(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "presence-not-position-client.toml");
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "presence-not-position-server.toml");
        PresenceNetwork.register(modBus);
        NeoForge.EVENT_BUS.addListener(ServerLocationTracker::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ServerLocationTracker::onLogout);
        NeoForge.EVENT_BUS.addListener(ServerLocationTracker::onDimensionChange);
        NeoForge.EVENT_BUS.addListener(ServerCommands::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
