package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScpPickupRouter {

    public static final String NO_MERGE_TAG = "ScpInventoryNoMerge";
    public static final String USABLE_SESSION_TAG = "ScpInventoryUsableSession";
    public static final String USABLE_START_TICK_TAG = "ScpInventoryUsableStartTick";
    public static final String COIN_MIRROR_TAG = "ScpInventoryCoinMirror";
    public static final int MAX_COIN_COUNT = 999;

    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;
    private static final Map<UUID, Integer> LAST_COIN_MIRROR_COUNT = new HashMap<>();

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty() || (player != null && player.isCreative())) {
            return 0;
        }

        stripNoMergeMarker(stack);

        ScpItemType type = ScpItemClassifier.getType(stack);

        if (type == ScpItemType.KEY) {
            return acceptKey(inventory, player, stack);
        }

        if (type == ScpItemType.CODEX) {
            return inventory.addDocumentItem(stack) ? stack.getCount() : 0;
        }

        int accepted = inventory.addInventoryItems(stack);
        if (accepted > 0 && player != null && type == ScpItemType.COIN) {
            syncCoinMirrors(player, inventory);
        }
        return accepted;
    }

    public static boolean reconcileCoinMirrors(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        UUID playerId = player.getUUID();
        boolean hadTracking = LAST_COIN_MIRROR_COUNT.containsKey(playerId);

        if (!hadTracking) {
            moveRealVanillaCoinsToCustomInventory(inventory, vanillaInventory);
            return syncCoinMirrors(player, inventory);
        }

        int mirrorCount = countCoinMirrors(vanillaInventory);
        int lastMirrorCount = LAST_COIN_MIRROR_COUNT.get(playerId);
        boolean changed = false;

        int mirrorDelta = mirrorCount - lastMirrorCount;
        if (mirrorDelta < 0) {
            changed |= removeCustomCoins(inventory, -mirrorDelta) > 0;
        } else if (mirrorDelta > 0) {
            changed |= addCustomCoinsFromMirror(inventory, mirrorDelta) > 0;
        }

        int acceptedRealCoins = moveRealVanillaCoinsToCustomInventory(inventory, vanillaInventory);
        if (acceptedRealCoins > 0) {
            changed = true;
        }

        int finalCustomCoinCount = countCustomCoins(inventory);
        int finalMirrorCount = countCoinMirrors(vanillaInventory);
        if (changed || finalMirrorCount != Math.min(finalCustomCoinCount, getMirrorCapacity(vanillaInventory))) {
            return syncCoinMirrors(player, inventory) || changed;
        }

        return changed;
    }

    public static boolean syncCoinMirrors(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        boolean changed = removeCoinMirrors(vanillaInventory);
        int placed = placeCoinMirrors(vanillaInventory, countCustomCoins(inventory));
        if (placed > 0) {
            changed = true;
        }

        LAST_COIN_MIRROR_COUNT.put(player.getUUID(), placed);
        if (changed) {
            syncVanillaInventory(player);
        }
        return changed;
    }

    public static void resetCoinMirrorTracking(ServerPlayer player) {
        if (player != null) {
            LAST_COIN_MIRROR_COUNT.remove(player.getUUID());
        }
    }

    private static int addCustomCoinsFromMirror(IScpInventory inventory, int amount) {
        ItemStack coin = ScpItemClassifier.getConfiguredCoinStack();
        if (coin.isEmpty() || amount <= 0) {
            return 0;
        }

        coin.setCount(amount);
        stripCoinMirror(coin);
        return inventory.addInventoryItems(coin);
    }

    private static int moveRealVanillaCoinsToCustomInventory(IScpInventory inventory, Inventory vanillaInventory) {
        int accepted = 0;
        accepted += moveRealCoinsFromList(inventory, vanillaInventory.items);
        accepted += moveRealCoinsFromList(inventory, vanillaInventory.offhand);
        accepted += moveRealCoinsFromList(inventory, vanillaInventory.armor);
        return accepted;
    }

    private static int moveRealCoinsFromList(IScpInventory inventory, List<ItemStack> stacks) {
        int acceptedTotal = 0;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack) || isCoinMirror(stack)) {
                continue;
            }

            ItemStack candidate = stack.copy();
            stripCoinMirror(candidate);
            int accepted = inventory.addInventoryItems(candidate);
            if (accepted <= 0) {
                continue;
            }

            stack.shrink(accepted);
            stacks.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            acceptedTotal += accepted;
        }
        return acceptedTotal;
    }

    private static int countCustomCoins(IScpInventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (!stack.isEmpty() && ScpItemClassifier.isCoin(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int removeCustomCoins(IScpInventory inventory, int amount) {
        int remaining = amount;
        for (int i = inventory.getMaxMainSlots() - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
                continue;
            }

            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            inventory.setInventoryItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            remaining -= removed;
        }
        return amount - remaining;
    }

    private static int countCoinMirrors(Inventory inventory) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (isCoinMirror(stack)) count += stack.getCount();
        }
        for (ItemStack stack : inventory.offhand) {
            if (isCoinMirror(stack)) count += stack.getCount();
        }
        for (ItemStack stack : inventory.armor) {
            if (isCoinMirror(stack)) count += stack.getCount();
        }
        return count;
    }

    private static boolean removeCoinMirrors(Inventory inventory) {
        boolean changed = false;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (isCoinMirror(inventory.items.get(i))) {
                inventory.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        for (int i = 0; i < inventory.offhand.size(); i++) {
            if (isCoinMirror(inventory.offhand.get(i))) {
                inventory.offhand.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        for (int i = 0; i < inventory.armor.size(); i++) {
            if (isCoinMirror(inventory.armor.get(i))) {
                inventory.armor.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        return changed;
    }

    private static int placeCoinMirrors(Inventory inventory, int amount) {
        ItemStack coinTemplate = ScpItemClassifier.getConfiguredCoinStack();
        if (coinTemplate.isEmpty() || amount <= 0) {
            return 0;
        }

        int placed = 0;
        int remaining = amount;
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());
        for (int i = end - 1; i >= VANILLA_MAIN_START && remaining > 0; i--) {
            if (!inventory.items.get(i).isEmpty()) {
                continue;
            }

            ItemStack mirror = coinTemplate.copy();
            mirror.setCount(Math.min(remaining, Math.min(mirror.getMaxStackSize(), inventory.getMaxStackSize())));
            markCoinMirror(mirror);
            inventory.setItem(i, mirror);
            remaining -= mirror.getCount();
            placed += mirror.getCount();
        }
        return placed;
    }

    private static int getMirrorCapacity(Inventory inventory) {
        int capacity = 0;
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());
        for (int i = VANILLA_MAIN_START; i < end; i++) {
            ItemStack stack = inventory.items.get(i);
            if (stack.isEmpty()) {
                ItemStack coin = ScpItemClassifier.getConfiguredCoinStack();
                capacity += coin.isEmpty() ? 0 : Math.min(coin.getMaxStackSize(), inventory.getMaxStackSize());
            } else if (isCoinMirror(stack)) {
                capacity += stack.getCount();
            }
        }
        return capacity;
    }

    public static void markCoinMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putBoolean(COIN_MIRROR_TAG, true);
    }

    public static boolean isCoinMirror(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(COIN_MIRROR_TAG);
    }

    public static void stripCoinMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(COIN_MIRROR_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static void syncVanillaInventory(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }

        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack copy = inventory.items.get(i).copy();
            player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, copy));
            player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, 0, toInventoryMenuSlot(i), copy));
        }
    }

    private static int toInventoryMenuSlot(int inventoryIndex) {
        return inventoryIndex >= 0 && inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    public static void markUsableSession(ItemStack stack, int startTick) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(USABLE_SESSION_TAG, true);
        tag.putInt(USABLE_START_TICK_TAG, startTick);
    }

    public static boolean isUsableSession(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(USABLE_SESSION_TAG);
    }

    public static int getUsableSessionStartTick(ItemStack stack) {
        if (!isUsableSession(stack) || stack.getTag() == null) {
            return 0;
        }
        return stack.getTag().getInt(USABLE_START_TICK_TAG);
    }

    public static void stripUsableSession(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        tag.remove(USABLE_SESSION_TAG);
        tag.remove(USABLE_START_TICK_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static int countCoins(Inventory inventory) {
        return countCoinMirrors(inventory);
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
