package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseHotbarItemPacket {

    private final int hotbarSlot;
    private final boolean continuousUse;

    public UseHotbarItemPacket(int hotbarSlot, boolean continuousUse) {
        this.hotbarSlot = hotbarSlot;
        this.continuousUse = continuousUse;
    }

    public static void encode(UseHotbarItemPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.hotbarSlot);
        buf.writeBoolean(msg.continuousUse);
    }

    public static UseHotbarItemPacket decode(FriendlyByteBuf buf) {
        return new UseHotbarItemPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(UseHotbarItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.activateUsableItem(msg.hotbarSlot, msg.continuousUse)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
