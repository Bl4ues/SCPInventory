package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class EquipmentActionPacket {

    public static final String ACTION_UNEQUIP = "UNEQUIP";
    public static final String ACTION_DROP = "DROP";

    private final String slotName;
    private final String action;

    public EquipmentActionPacket(String slotName, String action) {
        this.slotName = slotName == null ? "" : slotName;
        this.action = action == null ? "" : action;
    }

    public static void encode(EquipmentActionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.slotName);
        buf.writeUtf(msg.action);
    }

    public static EquipmentActionPacket decode(FriendlyByteBuf buf) {
        return new EquipmentActionPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(EquipmentActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Optional<ScpEquipmentSlot> slot = ScpEquipmentSlot.fromName(msg.slotName);
            if (slot.isEmpty()) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                switch (msg.action) {
                    case ACTION_UNEQUIP -> {
                        ItemStack equipped = inventory.getEquipment(slot.get());
                        if (equipped.isEmpty()) {
                            return;
                        }

                        if (inventory.addInventoryItem(equipped)) {
                            inventory.clearEquipment(slot.get());
                            InventoryActionPacket.syncVanillaEquipmentSlot(player, slot.get(), ItemStack.EMPTY);
                        } else {
                            ModNetwork.showInventoryFull(player);
                        }
                    }
                    case ACTION_DROP -> {
                        ItemStack equipped = inventory.extractEquipment(slot.get());
                        if (!equipped.isEmpty()) {
                            InventoryActionPacket.syncVanillaEquipmentSlot(player, slot.get(), ItemStack.EMPTY);
                            player.drop(equipped, false);
                        }
                    }
                    default -> {
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
