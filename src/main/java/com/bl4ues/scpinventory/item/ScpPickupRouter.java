package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ScpPickupRouter {

    public static final String NO_MERGE_TAG = "ScpInventoryNoMerge";
    public static final String USABLE_SESSION_TAG = "ScpInventoryUsableSession";
    public static final String USABLE_START_TICK_TAG = "ScpInventoryUsableStartTick";
    public static final String COIN_MIRROR_TAG = "ScpInventoryCoinMirror";
    public static final int MAX_COIN_COUNT = 999;

    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty() || (player != null && player.isCreative())) {
            return 0;
        }

        if (isCoinMirror(stack)) {
            return 0;
        }

        stripNoMergeMarker(stack);

        if (ScpItemClassifier.isCoin(stack)) {
            return acceptCoin(inventory, player, stack);
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

    public static boolean reconcileCoinMirrors(ServerPlayer player, IScpInventory inventory) {
        return false;
    }

    public static boolean sanitizeStoredCoinMirrors(IScpInventory inventory) {
        if (inventory == null) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (!stack.isEmpty() && isCoinMirror(stack)) {
                inventory.removeInventoryItem(i);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean syncCoinMirrors(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        boolean changed = removeTaggedCoinMirrors(vanillaInventory);
        int wanted = countCustomCoins(inventory);
        int existing = countPlainCoinMirrors(vanillaInventory);
        int missing = Math.max(0, wanted - existing);
        ItemStack template = ScpItemClassifier.getConfiguredCoinStack();

        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < vanillaInventory.items.size() && missing > 0 && !template.isEmpty(); i++) {
            if (!vanillaInventory.items.get(i).isEmpty()) {
                continue;
            }
            ItemStack mirror = template.copy();
            mirror.setCount(1);
            stripCoinMirror(mirror);
            vanillaInventory.items.set(i, mirror);
            vanillaInventory.setChanged();
            missing--;
            changed = true;
        }

        if (changed) {
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    public static void resetCoinMirrorTracking(ServerPlayer player) {
    }

    private static int acceptCoin(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (player == null) {
            return 0;
        }

        int acceptedLimit = Math.min(stack.getCount(), inventory.getFreeMainSlots());
        acceptedLimit = Math.min(acceptedLimit, getFreeCoinMirrorSlots(player));
        acceptedLimit = Math.min(acceptedLimit, 1);

        int accepted = 0;
        for (int i = 0; i < acceptedLimit; i++) {
            ItemStack singleCoin = stack.copy();
            singleCoin.setCount(1);
            stripNoMergeMarker(singleCoin);
            stripCoinMirror(singleCoin);

            if (!addMirroredCoin(player, singleCoin)) {
                break;
            }

            if (inventory.addInventoryItems(singleCoin) <= 0) {
                removeMirroredCoin(player, singleCoin);
                break;
            }

            accepted++;
        }

        return accepted;
    }

    private static int getFreeCoinMirrorSlots(ServerPlayer player) {
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

    private static boolean addMirroredCoin(ServerPlayer player, ItemStack coin) {
        if (player == null || coin == null || coin.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                ItemStack copy = coin.copy();
                copy.setCount(1);
                stripCoinMirror(copy);
                inventory.items.set(i, copy);
                inventory.setChanged();
                player.containerMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    private static boolean removeMirroredCoin(ServerPlayer player, ItemStack coin) {
        if (player == null || coin == null || coin.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (sameCoin(candidate, coin)) {
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

    private static boolean sameCoin(ItemStack candidate, ItemStack coin) {
        if (candidate == null || candidate.isEmpty() || coin == null || coin.isEmpty()) {
            return false;
        }
        ItemStack normalizedCandidate = candidate.copy();
        normalizedCandidate.setCount(1);
        stripCoinMirror(normalizedCandidate);

        ItemStack normalizedCoin = coin.copy();
        normalizedCoin.setCount(1);
        stripCoinMirror(normalizedCoin);

        return ItemStack.isSameItemSameTags(normalizedCandidate, normalizedCoin);
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

    private static int countPlainCoinMirrors(Inventory inventory) {
        int count = 0;
        ItemStack template = ScpItemClassifier.getConfiguredCoinStack();
        if (inventory == null || template.isEmpty()) {
            return 0;
        }
        ItemStack expected = template.copy();
        expected.setCount(1);
        stripCoinMirror(expected);
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (sameCoin(stack, expected)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean removeTaggedCoinMirrors(Inventory inventory) {
        boolean changed = false;
        for (int i = 0; i < inventory.items.size(); i++) {
            if (isCoinMirror(inventory.items.get(i))) {
                inventory.items.set(i, ItemStack.EMPTY);
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
        if (changed) {
            inventory.setChanged();
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

    public static int countCoins(Inventory inventory) {
        return countPlainCoinMirrors(inventory);
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
