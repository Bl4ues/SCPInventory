package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.InventoryFullOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class InventoryFullPacket {

    public InventoryFullPacket() {
    }

    public static void encode(InventoryFullPacket msg, FriendlyByteBuf buf) {
        // Sem dados para enviar
    }

    public static InventoryFullPacket decode(FriendlyByteBuf buf) {
        return new InventoryFullPacket();
    }

    public static void handle(InventoryFullPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            InventoryFullOverlay.show();
        });
        ctx.get().setPacketHandled(true);
    }
}