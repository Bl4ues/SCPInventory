package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
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
    private static final Map<UUID, Integer> LAST_MIRROR_COUNT = new HashMap<>();

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty() || (player != null && player.isCreative())) {
            return 0;
        }

        stripNoMergeMarker(stack);

        ScpItemType type = ScpItemClassifier.getType(stack);

        if (type == ScpItemType.COIN) {
            return acceptCoinStack(inventory, player, stack);
        }

        if (type == ScpItemType.KEY) {
            return acceptKey(inventory, player, stack);
        }

        if (type == ScpItemType.CODEX) {
            return inventory.addDocumentItem(stack) ? stack.getCount() : 0;
        }

        return inventory.addInventoryItems(stack);
    }

    public static int acceptCoinStack(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || player == null || player.isCreative() || stack == null || stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
            return 0;
        }

        int freeCoinSpace = MAX_COIN_COUNT - inventory.getCoinCount();
        if (freeCoinSpace <= 0) {
            syncCoinMirror(player, inventory);
            return 0;
        }

        int accepted = Math.min(stack.getCount(), freeCoinSpace);
        if (accepted <= 0) {
            return 0;
        }

        inventory.setCoinCount(inventory.getCoinCount() + accepted);
        syncCoinMirror(player, inventory);
        return accepted;
    }

    public static boolean reconcileCoinMirror(ServerPlayer player, IScpInventory scpInventory) {
        if (player == null || scpInventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        int vanillaCount = countAllConfiguredCoins(player.getInventory());
        int lastMirrorCount = LAST_MIRROR_COUNT.getOrDefault(player.getUUID(), vanillaCount);
        int delta = vanillaCount - lastMirrorCount;
        boolean changed = false;

        if (delta != 0) {
            scpInventory.setCoinCount(scpInventory.getCoinCount() + delta);
            changed = true;
        }

        boolean mirrorChanged = syncCoinMirror(player, scpInventory);
        return changed || mirrorChanged;
    }

    public static boolean syncCoinMirror(ServerPlayer player, IScpInventory scpInventory) {
        if (player == null || scpInventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        boolean changed = removeAllConfiguredCoins(inventory);

        ItemStack coinTemplate = ScpItemClassifier.getConfiguredCoinStack();
        int remaining = Math.max(0, Math.min(MAX_COIN_COUNT, scpInventory.getCoinCount()));
        int placed = 0;
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());

        if (!coinTemplate.isEmpty() && remaining > 0) {
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
                changed = true;
            }
        }

        LAST_MIRROR_COUNT.put(player.getUUID(), placed);
        if (changed) {
            syncVanillaInventory(player);
        }
        return changed;
    }

    public static void resetCoinMirrorTracking(ServerPlayer player) {
        if (player != null) {
            LAST_MIRROR_COUNT.remove(player.getUUID());
        }
    }

    private static int countAllConfiguredCoins(Inventory inventory) {
        if (inventory == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && ScpItemClassifier.isCoin(stack)) count += stack.getCount();
        }
        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty() && ScpItemClassifier.isCoin(stack)) count += stack.getCount();
        }
        for (ItemStack stack : inventory.armor) {
            if (!stack.isEmpty() && ScpItemClassifier.isCoin(stack)) count += stack.getCount();
        }
        return count;
    }

    private static boolean removeAllConfiguredCoins(Inventory inventory) {
        boolean changed = false;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (!inventory.items.get(i).isEmpty() && ScpItemClassifier.isCoin(inventory.items.get(i))) {
                inventory.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        for (int i = 0; i < inventory.offhand.size(); i++) {
            if (!inventory.offhand.get(i).isEmpty() && ScpItemClassifier.isCoin(inventory.offhand.get(i))) {
                inventory.offhand.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        for (int i = 0; i < inventory.armor.size(); i++) {
            if (!inventory.armor.get(i).isEmpty() && ScpItemClassifier.isCoin(inventory.armor.get(i))) {
                inventory.armor.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        return changed;
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
        return countAllConfiguredCoins(inventory);
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
