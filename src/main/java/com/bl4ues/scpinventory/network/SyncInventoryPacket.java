package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncInventoryPacket {

    private final CompoundTag inventoryTag;

    public SyncInventoryPacket(CompoundTag inventoryTag) {
        this.inventoryTag = inventoryTag == null ? new CompoundTag() : inventoryTag.copy();
    }

    public static void encode(SyncInventoryPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.inventoryTag);
    }

    public static SyncInventoryPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncInventoryPacket(tag);
    }

    public static void handle(SyncInventoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.syncInventory(msg.inventoryTag)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
