package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ScpPickupRouter {

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty() || (player != null && player.isCreative())) {
            return 0;
        }

        ScpItemType type = ScpItemClassifier.getType(stack);

        if (type == ScpItemType.KEY) {
            return acceptKey(inventory, player, stack);
        }

        if (type == ScpItemType.CODEX) {
            return inventory.addDocumentItem(stack) ? stack.getCount() : 0;
        }

        return inventory.addInventoryItems(stack);
    }

    private static int acceptKey(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (player == null) {
            return 0;
        }

        int acceptedLimit = Math.min(stack.getCount(), inventory.getFreeKeySlots());
        acceptedLimit = Math.min(acceptedLimit, ScpKeyringMirror.getFreeMirrorSlots(player));

        int accepted = 0;
        for (int i = 0; i < acceptedLimit; i++) {
            ItemStack singleKey = stack.copy();
            singleKey.setCount(1);

            if (!ScpKeyringMirror.addMirroredKey(player, singleKey)) {
                break;
            }

            if (!inventory.addKeyItem(singleKey)) {
                ScpKeyringMirror.removeMirroredKey(player, singleKey);
                break;
            }

            accepted++;
        }

        return accepted;
    }
}
