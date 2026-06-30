package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpInventoryMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(
                id++,
                InventoryFullPacket.class,
                InventoryFullPacket::encode,
                InventoryFullPacket::decode,
                InventoryFullPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                SyncInventoryPacket.class,
                SyncInventoryPacket::encode,
                SyncInventoryPacket::decode,
                SyncInventoryPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                RequestInventorySyncPacket.class,
                RequestInventorySyncPacket::encode,
                RequestInventorySyncPacket::decode,
                RequestInventorySyncPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                InventoryActionPacket.class,
                InventoryActionPacket::encode,
                InventoryActionPacket::decode,
                InventoryActionPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                EquipmentActionPacket.class,
                EquipmentActionPacket::encode,
                EquipmentActionPacket::decode,
                EquipmentActionPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                KeyActionPacket.class,
                KeyActionPacket::encode,
                KeyActionPacket::decode,
                KeyActionPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                InventoryMovePacket.class,
                InventoryMovePacket::encode,
                InventoryMovePacket::decode,
                InventoryMovePacket::handle
        );

        CHANNEL.registerMessage(
                id,
                PickupItemPacket.class,
                PickupItemPacket::encode,
                PickupItemPacket::decode,
                PickupItemPacket::handle
        );
    }

    public static void syncTo(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null) {
            return;
        }

        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncInventoryPacket(inventory.serializeNBT())
        );
    }

    public static void showInventoryFull(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new InventoryFullPacket());
    }
}
