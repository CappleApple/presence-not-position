package com.cappleapple.presencenotposition.client;

import com.cappleapple.presencenotposition.client.music.ClientMusicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class ClientCommands {
    private ClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pnp")
            .then(Commands.literal("music")
                .then(Commands.literal("current").executes(context -> message(context.getSource(), ClientMusicManager.debugComponent())))
                .then(Commands.literal("next").executes(context -> {
                    ClientMusicManager.next();
                    return message(context.getSource(), Component.translatable("commands.presencenotposition.music.next"));
                }))
                .then(Commands.literal("stop").executes(context -> {
                    ClientMusicManager.stopManual();
                    return message(context.getSource(), Component.translatable("commands.presencenotposition.music.stop"));
                }))
                .then(Commands.literal("reload").executes(context -> {
                    Minecraft.getInstance().reloadResourcePacks();
                    return message(context.getSource(), Component.translatable("commands.presencenotposition.music.reload"));
                })))
            .then(Commands.literal("history").executes(context -> message(context.getSource(),
                Component.translatable("commands.presencenotposition.history", PresentationHistory.size()))))
            .then(Commands.literal("clearhistory").executes(context -> {
                PresentationHistory.clear();
                return message(context.getSource(), Component.translatable("commands.presencenotposition.history.cleared"));
            })));
    }

    private static int message(net.minecraft.commands.CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
        return 1;
    }
}
