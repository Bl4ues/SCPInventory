package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.network.EquipmentActionPacket;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.InventoryMovePacket;
import com.bl4ues.scpinventory.network.KeyActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ClientInventoryBridge {

    private ClientInventoryBridge() {
    }

    public static void perform(int slot, String name) {
        closeScreenBeforeUsableAction(slot, name);
        ModNetwork.CHANNEL.sendToServer(new InventoryActionPacket(slot, name));
    }

    public static void performKey(int index, String name) {
        ModNetwork.CHANNEL.sendToServer(new KeyActionPacket(index, name));
    }

    public static void performEquipment(ScpEquipmentSlot slot, String name) {
        if (slot == null) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new EquipmentActionPacket(slot.name(), name));
    }

    public static void moveMainToMain(int sourceIndex, int targetIndex) {
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_MAIN,
                sourceIndex,
                "",
                InventoryMovePacket.PLACE_MAIN,
                targetIndex,
                ""
        ));
    }

    public static void moveMainToEquipment(int sourceIndex, ScpEquipmentSlot targetSlot) {
        if (targetSlot == null) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_MAIN,
                sourceIndex,
                "",
                InventoryMovePacket.PLACE_EQUIPMENT,
                -1,
                targetSlot.name()
        ));
    }

    public static void moveMainToWorld(int sourceIndex) {
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_MAIN,
                sourceIndex,
                "",
                InventoryMovePacket.PLACE_WORLD,
                -1,
                ""
        ));
    }

    public static void moveEquipmentToMain(ScpEquipmentSlot sourceSlot, int targetIndex) {
        if (sourceSlot == null) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_EQUIPMENT,
                -1,
                sourceSlot.name(),
                InventoryMovePacket.PLACE_MAIN,
                targetIndex,
                ""
        ));
    }

    public static void moveEquipmentToEquipment(ScpEquipmentSlot sourceSlot, ScpEquipmentSlot targetSlot) {
        if (sourceSlot == null || targetSlot == null) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_EQUIPMENT,
                -1,
                sourceSlot.name(),
                InventoryMovePacket.PLACE_EQUIPMENT,
                -1,
                targetSlot.name()
        ));
    }

    public static void moveEquipmentToWorld(ScpEquipmentSlot sourceSlot) {
        if (sourceSlot == null) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_EQUIPMENT,
                -1,
                sourceSlot.name(),
                InventoryMovePacket.PLACE_WORLD,
                -1,
                ""
        ));
    }

    private static void closeScreenBeforeUsableAction(int slot, String action) {
        if (!InventoryActionPacket.ACTION_USE.equals(action)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        mc.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            if (!inventory.isValidMainSlot(slot)) {
                return;
            }

            ItemStack stack = inventory.getInventoryItem(slot);
            if (!stack.isEmpty() && ScpItemClassifier.getType(stack) == ScpItemType.USABLE && hasEmptyHotbarSlot(mc.player.getInventory())) {
                mc.setScreen(null);
                UsableItemHoldClient.start();
            }
        });
    }

    private static boolean hasEmptyHotbarSlot(Inventory inventory) {
        for (int i = 0; i < 9 && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
