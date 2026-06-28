package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.world.item.ItemStack;

public final class ScpPickupRouter {

    private ScpPickupRouter() {
    }

    public static boolean accept(IScpInventory inventory, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty()) {
            return false;
        }

        ScpItemType type = ScpItemClassifier.getType(stack);

        if (type == ScpItemType.KEY) {
            return inventory.addKeyItem(stack);
        }

        if (type == ScpItemType.CODEX) {
            return inventory.addDocumentItem(stack);
        }

        return inventory.addInventoryItem(stack);
    }
}
