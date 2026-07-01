package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

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

    public static void activateUsableItem(int hotbarSlot, boolean continuousUse) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }

        minecraft.setScreen(null);
        UsableItemHoldClient.start(hotbarSlot, continuousUse);
    }
}
