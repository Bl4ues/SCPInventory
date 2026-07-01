package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ScpPickupRouter {

    public static final String NO_MERGE_TAG = "ScpInventoryNoMerge";
    public static final int MAX_COIN_COUNT = 999;

    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty() || (player != null && player.isCreative())) {
            return 0;
        }

        stripNoMergeMarker(stack);

        ScpItemType type = ScpItemClassifier.getType(stack);

        if (type == ScpItemType.COIN) {
            return acceptCoinStack(player, stack);
        }

        if (type == ScpItemType.KEY) {
            return acceptKey(inventory, player, stack);
        }

        if (type == ScpItemType.CODEX) {
            return inventory.addDocumentItem(stack) ? stack.getCount() : 0;
        }

        return inventory.addInventoryItems(stack);
    }

    public static int acceptCoinStack(ServerPlayer player, ItemStack stack) {
        if (player == null || player.isCreative() || stack == null || stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
            return 0;
        }

        Inventory inventory = player.getInventory();
        int freeCoinSpace = MAX_COIN_COUNT - countCoins(inventory);
        if (freeCoinSpace <= 0) {
            return 0;
        }

        ItemStack remaining = stack.copy();
        stripNoMergeMarker(remaining);
        remaining.setCount(Math.min(remaining.getCount(), freeCoinSpace));
        int startingCount = remaining.getCount();

        mergeCoinIntoExistingMainStacks(inventory, remaining);
        placeCoinIntoEmptyMainSlots(inventory, remaining);

        int accepted = startingCount - remaining.getCount();
        if (accepted > 0) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }

        return accepted;
    }

    public static int countCoins(Inventory inventory) {
        if (inventory == null) {
            return 0;
        }

        int count = 0;
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());
        for (int i = VANILLA_MAIN_START; i < end; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && ScpItemClassifier.isCoin(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static void addNoMergeMarker(ItemStack stack, String marker) {
        if (stack == null || stack.isEmpty() || marker == null || marker.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putString(NO_MERGE_TAG, marker);
    }

    public static void stripNoMergeMarker(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NO_MERGE_TAG)) {
            return;
        }

        tag.remove(NO_MERGE_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private static void mergeCoinIntoExistingMainStacks(Inventory inventory, ItemStack remaining) {
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());
        for (int i = end - 1; i >= VANILLA_MAIN_START && !remaining.isEmpty(); i--) {
            ItemStack candidate = inventory.items.get(i);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(candidate, remaining)) {
                continue;
            }

            int space = Math.min(candidate.getMaxStackSize(), inventory.getMaxStackSize()) - candidate.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            candidate.grow(moved);
            remaining.shrink(moved);
        }
    }

    private static void placeCoinIntoEmptyMainSlots(Inventory inventory, ItemStack remaining) {
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());
        for (int i = end - 1; i >= VANILLA_MAIN_START && !remaining.isEmpty(); i--) {
            if (!inventory.items.get(i).isEmpty()) {
                continue;
            }

            ItemStack inserted = remaining.copy();
            inserted.setCount(Math.min(remaining.getCount(), Math.min(inserted.getMaxStackSize(), inventory.getMaxStackSize())));
            inventory.items.set(i, inserted);
            remaining.shrink(inserted.getCount());
        }
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
            stripNoMergeMarker(singleKey);

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
