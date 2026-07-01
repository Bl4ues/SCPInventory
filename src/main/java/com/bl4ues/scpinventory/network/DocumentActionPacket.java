package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DocumentActionPacket {

    public static final String ACTION_DROP = "DROP";

    private final int index;
    private final String action;

    public DocumentActionPacket(int index, String action) {
        this.index = index;
        this.action = action == null ? "" : action;
    }

    public static void encode(DocumentActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.index);
        buf.writeUtf(msg.action);
    }

    public static DocumentActionPacket decode(FriendlyByteBuf buf) {
        return new DocumentActionPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(DocumentActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (ACTION_DROP.equals(msg.action)) {
                    ItemStack document = inventory.extractDocumentItem(msg.index);
                    if (!document.isEmpty()) {
                        player.drop(document, false);
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
