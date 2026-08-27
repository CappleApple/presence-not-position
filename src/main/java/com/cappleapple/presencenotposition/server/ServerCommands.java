package com.cappleapple.presencenotposition.server;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationTransition;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ServerCommands {
    private static final Set<UUID> DEBUG_PLAYERS = new HashSet<>();

    private ServerCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pnp")
            .then(Commands.literal("debug").requires(source -> source.hasPermission(2))
                .executes(context -> toggleDebug(context.getSource().getPlayerOrException())))
            .then(Commands.literal("title").requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (LocationType type : LocationType.values()) builder.suggest(type.name().toLowerCase(Locale.ROOT));
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("id", ResourceLocationArgument.id())
                        .executes(context -> show(
                            context.getSource().getPlayerOrException(),
                            LocationType.parse(StringArgumentType.getString(context, "type")),
                            ResourceLocationArgument.getId(context, "id")
                        ))))));
    }

    private static int toggleDebug(ServerPlayer player) {
        boolean enabled = DEBUG_PLAYERS.add(player.getUUID());
        if (!enabled) DEBUG_PLAYERS.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable(enabled
            ? "commands.presencenotposition.debug.enabled"
            : "commands.presencenotposition.debug.disabled"));
        return 1;
    }

    private static int show(ServerPlayer player, LocationType type, ResourceLocation id) {
        LocationContext context = new LocationContext(type, id);
        PresentationService.show(player, context, PresentationOverride.NONE);
        player.sendSystemMessage(Component.translatable("commands.presencenotposition.title.sent",
            type.name().toLowerCase(Locale.ROOT), id.toString()));
        return 1;
    }

    static void debugTransition(ServerPlayer player, LocationTransition transition) {
        if (DEBUG_PLAYERS.contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("[PNP] " + (transition.entered() ? "ENTER " : "EXIT ") + transition.context()));
        }
    }
}
