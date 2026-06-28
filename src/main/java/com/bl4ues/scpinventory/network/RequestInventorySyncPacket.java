package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestInventorySyncPacket {

    public RequestInventorySyncPacket() {
    }

    public static void encode(RequestInventorySyncPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestInventorySyncPacket decode(FriendlyByteBuf buf) {
        return new RequestInventorySyncPacket();
    }

    public static void handle(RequestInventorySyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
                    ModNetwork.syncTo(player, inventory)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
