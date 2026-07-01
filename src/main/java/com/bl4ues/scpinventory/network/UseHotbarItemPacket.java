package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseHotbarItemPacket {

    private final int hotbarSlot;
    private final boolean continuousUse;
    private final ItemStack stack;

    public UseHotbarItemPacket(int hotbarSlot, boolean continuousUse, ItemStack stack) {
        this.hotbarSlot = hotbarSlot;
        this.continuousUse = continuousUse;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public static void encode(UseHotbarItemPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.hotbarSlot);
        buf.writeBoolean(msg.continuousUse);
        buf.writeItem(msg.stack);
    }

    public static UseHotbarItemPacket decode(FriendlyByteBuf buf) {
        return new UseHotbarItemPacket(buf.readInt(), buf.readBoolean(), buf.readItem());
    }

    public static void handle(UseHotbarItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.activateUsableItem(msg.hotbarSlot, msg.continuousUse, msg.stack)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
