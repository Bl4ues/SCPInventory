package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpKeyringMirror;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KeyActionPacket {

    public static final String ACTION_DROP = "DROP";

    private final int index;
    private final String action;

    public KeyActionPacket(int index, String action) {
        this.index = index;
        this.action = action == null ? "" : action;
    }

    public static void encode(KeyActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.index);
        buf.writeUtf(msg.action);
    }

    public static KeyActionPacket decode(FriendlyByteBuf buf) {
        return new KeyActionPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(KeyActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (ACTION_DROP.equals(msg.action)) {
                    ItemStack key = inventory.extractKeyItem(msg.index);
                    if (!key.isEmpty()) {
                        ScpKeyringMirror.removeMirroredKey(player, key);
                        player.drop(key, false);
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
