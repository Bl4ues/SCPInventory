package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void showInventoryFullOverlay() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && (minecraft.player.isCreative() || minecraft.player.isSpectator())) {
            InventoryFullOverlay.hide();
            return;
        }
        InventoryFullOverlay.show();
    }

    public static void syncInventory(CompoundTag inventoryTag) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || inventoryTag == null) {
            return;
        }

        minecraft.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
                inventory.deserializeNBT(inventoryTag.copy())
        );
    }

    public static void activateUsableItem(int hotbarSlot, boolean continuousUse, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }

        if (hotbarSlot >= 0 && hotbarSlot < 9 && hotbarSlot < minecraft.player.getInventory().items.size() && stack != null && !stack.isEmpty()) {
            minecraft.player.getInventory().items.set(hotbarSlot, stack.copy());
            minecraft.player.getInventory().selected = hotbarSlot;
        }

        minecraft.setScreen(null);
        UsableItemHoldClient.start(hotbarSlot, continuousUse);
    }
}
