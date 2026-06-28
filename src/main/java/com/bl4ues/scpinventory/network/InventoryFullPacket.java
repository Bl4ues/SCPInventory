package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class InventoryFullPacket {

    public InventoryFullPacket() {
    }

    public static void encode(InventoryFullPacket msg, FriendlyByteBuf buf) {
    }

    public static InventoryFullPacket decode(FriendlyByteBuf buf) {
        return new InventoryFullPacket();
    }

    public static void handle(InventoryFullPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.showInventoryFullOverlay()
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
