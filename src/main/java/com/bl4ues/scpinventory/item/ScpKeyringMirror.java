package com.bl4ues.scpinventory.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class ScpKeyringMirror {

    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;

    private ScpKeyringMirror() {
    }

    public static int getFreeMirrorSlots(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        int free = 0;
        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                free++;
            }
        }

        return free;
    }

    public static boolean addMirroredKey(ServerPlayer player, ItemStack key) {
        if (player == null || key == null || key.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                ItemStack copy = key.copy();
                copy.setCount(1);
                inventory.items.set(i, copy);
                inventory.setChanged();
                player.containerMenu.broadcastChanges();
                return true;
            }
        }

        return false;
    }

    public static boolean removeMirroredKey(ServerPlayer player, ItemStack key) {
        if (player == null || key == null || key.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (sameKey(candidate, key)) {
                candidate.shrink(1);
                if (candidate.isEmpty()) {
                    inventory.items.set(i, ItemStack.EMPTY);
                }
                inventory.setChanged();
                player.containerMenu.broadcastChanges();
                return true;
            }
        }

        return false;
    }

    public static void removeMirroredKeys(ServerPlayer player, List<ItemStack> keys) {
        if (player == null || keys == null) {
            return;
        }

        for (ItemStack key : keys) {
            removeMirroredKey(player, key);
        }
    }

    private static boolean sameKey(ItemStack candidate, ItemStack key) {
        return candidate != null
                && !candidate.isEmpty()
                && ItemStack.isSameItemSameTags(candidate, key);
    }
}
