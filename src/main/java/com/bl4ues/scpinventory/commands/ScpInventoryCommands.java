package com.bl4ues.scpinventory.commands;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public class ScpInventoryCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("scpinventory")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reset")
                                .executes(context -> resetSingle(context.getSource().getPlayerOrException()))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> resetMany(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")
                                        ))
                                )
                        )
                        .then(Commands.literal("clear")
                                .executes(context -> clearMainSingle(context.getSource().getPlayerOrException()))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> clearMainMany(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")
                                        ))
                                )
                        )
                        .then(Commands.literal("clearmain")
                                .executes(context -> clearMainSingle(context.getSource().getPlayerOrException()))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> clearMainMany(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")
                                        ))
                                )
                        )
                        .then(Commands.literal("setmax")
                                .then(Commands.argument("slots", IntegerArgumentType.integer(1, 128))
                                        .executes(context -> setMaxSingle(
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "slots")
                                        ))
                                )
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("slots", IntegerArgumentType.integer(1, 128))
                                                .executes(context -> setMaxMany(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "slots")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("getmax")
                                .executes(context -> getMax(context.getSource().getPlayerOrException()))
                        )
        );
    }

    private static int resetSingle(ServerPlayer player) {
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            inventory.resetAll();
            ModNetwork.syncTo(player, inventory);
        });

        player.sendSystemMessage(Component.literal("SCP Inventory reset. Max main slots restored to 12."));
        return 1;
    }

    private static int resetMany(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            resetSingle(player);
        }

        source.sendSuccess(() -> Component.literal("Reset SCP Inventory for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int clearMainSingle(ServerPlayer player) {
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            inventory.resetMainInventory();
            ModNetwork.syncTo(player, inventory);
        });

        player.sendSystemMessage(Component.literal("SCP main inventory cleared."));
        return 1;
    }

    private static int clearMainMany(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            clearMainSingle(player);
        }

        source.sendSuccess(() -> Component.literal("Cleared SCP main inventory for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int setMaxSingle(ServerPlayer player, int slots) {
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            inventory.setMaxMainSlots(slots);
            ModNetwork.syncTo(player, inventory);
            player.sendSystemMessage(Component.literal("SCP max main slots set to " + inventory.getMaxMainSlots() + "."));
        });

        return 1;
    }

    private static int setMaxMany(CommandSourceStack source, Collection<ServerPlayer> players, int slots) {
        for (ServerPlayer player : players) {
            setMaxSingle(player, slots);
        }

        source.sendSuccess(() -> Component.literal("Set SCP max main slots to " + slots + " for " + players.size() + " player(s)."), true);
        return players.size();
    }

    private static int getMax(ServerPlayer player) {
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
                player.sendSystemMessage(Component.literal(
                        "SCP main slots: " + inventory.getInventoryCount() + "/" + inventory.getMaxMainSlots()
                ))
        );

        return 1;
    }
}
