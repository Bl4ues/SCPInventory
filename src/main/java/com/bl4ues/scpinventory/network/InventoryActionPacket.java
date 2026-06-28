package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class InventoryActionPacket {

    public static final String ACTION_DROP = "DROP";
    public static final String ACTION_USE = "USE";
    public static final String ACTION_EQUIP = "EQUIP";

    private final int slot;
    private final String action;

    public InventoryActionPacket(int slot, String action) {
        this.slot = slot;
        this.action = action == null ? "" : action;
    }

    public static void encode(InventoryActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
        buf.writeUtf(msg.action);
    }

    public static InventoryActionPacket decode(FriendlyByteBuf buf) {
        return new InventoryActionPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(InventoryActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (!inventory.isValidMainSlot(msg.slot)) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                switch (msg.action) {
                    case ACTION_DROP -> moveSlotToWorld(player, inventory, msg.slot);
                    case ACTION_USE -> useSlot(player, inventory, msg.slot);
                    case ACTION_EQUIP -> equipSlot(inventory, msg.slot);
                    default -> {
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void moveSlotToWorld(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.extractInventoryItem(slot);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static void useSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty() || ScpItemClassifier.getType(stack) != ScpItemType.CONSUMABLE) {
            return;
        }

        if (stack.isEdible()) {
            ItemStack result = player.eat(player.level(), stack.copy());
            inventory.setInventoryItem(slot, result);
        } else {
            inventory.removeInventoryItem(slot);
        }
    }

    private static void equipSlot(IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty()) {
            return;
        }

        Optional<ScpEquipmentSlot> equipmentSlot = ScpItemClassifier.getEquipmentSlot(stack);
        if (equipmentSlot.isEmpty()) {
            return;
        }

        ScpEquipmentSlot targetSlot = equipmentSlot.get();
        ItemStack newEquipment = inventory.extractInventoryItem(slot);
        ItemStack previousEquipment = inventory.getEquipment(targetSlot);

        inventory.setEquipment(targetSlot, newEquipment);

        if (!previousEquipment.isEmpty()) {
            inventory.setInventoryItem(slot, previousEquipment);
        }
    }
}
