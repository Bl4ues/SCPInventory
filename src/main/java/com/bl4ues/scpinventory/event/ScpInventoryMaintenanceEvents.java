package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class ScpInventoryMaintenanceEvents {

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;
    private static final int USABLE_RETURN_GRACE_TICKS = 12;
    private static final long COIN_MESSAGE_COOLDOWN_MS = 2500L;
    private static final Map<UUID, Long> LAST_COIN_MESSAGE_MS = new HashMap<>();

    private ScpInventoryMaintenanceEvents() {
    }

    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        if (!ScpItemClassifier.isCoin(stack)) {
            return;
        }

        // Coins must use the SCP manual pickup flow, just like the rest of the custom inventory items.
        // This only blocks vanilla proximity pickup; PickupItemPacket handles the actual manual pickup.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }

        ItemStack tossedStack = event.getEntity().getItem();
        if (!ScpItemClassifier.isCoin(tossedStack)) {
            return;
        }

        int originalCount = tossedStack.getCount();
        int accepted = ScpPickupRouter.acceptCoinStack(player, tossedStack.copy());
        if (accepted >= originalCount) {
            event.setCanceled(true);
            return;
        }

        if (accepted > 0) {
            ItemStack remainder = tossedStack.copy();
            remainder.setCount(originalCount - accepted);
            event.getEntity().setItem(remainder);
            showCoinCapMessage(player);
            return;
        }

        // If the player is already at the cap, do not cancel the toss: canceling here deletes coins
        // dropped from containers. The manual pickup/cap rules will still prevent free collection.
        showCoinCapMessage(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isSpectator()) {
            return;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            boolean changed = false;
            if (!player.isCreative()) {
                changed |= enforceCoinCap(player);
                changed |= migrateCoinsFromCustomInventory(player, inventory);
                changed |= moveCoinsOutOfInvalidVanillaSlots(player);
                changed |= maintainUsableSessions(player, inventory);
            }
            changed |= reconcileAccessoryHand(player, inventory);
            if (changed) {
                ModNetwork.syncTo(player, inventory);
            }
        });
    }

    private static boolean maintainUsableSessions(ServerPlayer player, IScpInventory inventory) {
        boolean changed = false;
        Inventory vanillaInventory = player.getInventory();
        int end = Math.min(VANILLA_HOTBAR_END_EXCLUSIVE, vanillaInventory.items.size());

        for (int slot = VANILLA_HOTBAR_START; slot < end; slot++) {
            ItemStack stack = vanillaInventory.items.get(slot);
            if (stack.isEmpty() || !ScpPickupRouter.isUsableSession(stack)) {
                continue;
            }

            int age = player.tickCount - ScpPickupRouter.getUsableSessionStartTick(stack);
            if (vanillaInventory.selected == slot && shouldKeepUsableInHand(player, stack, age)) {
                continue;
            }

            changed |= returnUsableSessionToCustomInventory(player, inventory, vanillaInventory, slot, stack);
        }

        return changed;
    }

    private static boolean shouldKeepUsableInHand(ServerPlayer player, ItemStack stack, int age) {
        if (age < USABLE_RETURN_GRACE_TICKS) {
            return true;
        }

        if (stack.is(Items.FISHING_ROD) && player.fishing != null) {
            return true;
        }

        return player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND;
    }

    private static boolean returnUsableSessionToCustomInventory(ServerPlayer player, IScpInventory inventory, Inventory vanillaInventory, int hotbarSlot, ItemStack stack) {
        ItemStack returning = stack.copy();
        ScpPickupRouter.stripUsableSession(returning);
        ScpPickupRouter.stripNoMergeMarker(returning);

        if (returning.isEmpty()) {
            vanillaInventory.setItem(hotbarSlot, ItemStack.EMPTY);
            ScpPickupRouter.syncVanillaInventory(player);
            return true;
        }

        int accepted = inventory.addInventoryItems(returning.copy());
        if (accepted > 0) {
            if (accepted >= returning.getCount()) {
                vanillaInventory.setItem(hotbarSlot, ItemStack.EMPTY);
            } else {
                ItemStack remainder = stack.copy();
                remainder.shrink(accepted);
                vanillaInventory.setItem(hotbarSlot, remainder);
            }
            ScpPickupRouter.syncVanillaInventory(player);
            return true;
        }

        ModNetwork.showInventoryFull(player);
        return false;
    }

    private static boolean migrateCoinsFromCustomInventory(ServerPlayer player, IScpInventory inventory) {
        boolean changed = false;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
                continue;
            }

            int accepted = ScpPickupRouter.acceptCoinStack(player, stack.copy());
            if (accepted >= stack.getCount()) {
                inventory.removeInventoryItem(i);
                changed = true;
            } else if (accepted > 0) {
                stack.shrink(accepted);
                inventory.setInventoryItem(i, stack);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean moveCoinsOutOfInvalidVanillaSlots(ServerPlayer player) {
        boolean changed = false;
        Inventory inventory = player.getInventory();

        for (int i = VANILLA_HOTBAR_START; i < VANILLA_HOTBAR_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
                continue;
            }

            changed |= moveCoinStackToMainInventory(player, inventory.items, i, stack);
        }

        for (int i = 0; i < inventory.offhand.size(); i++) {
            ItemStack stack = inventory.offhand.get(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
                continue;
            }

            changed |= moveCoinStackToMainInventory(player, inventory.offhand, i, stack);
        }

        for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = inventory.armor.get(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
                continue;
            }

            changed |= moveCoinStackToMainInventory(player, inventory.armor, i, stack);
        }

        if (changed) {
            ScpPickupRouter.syncVanillaInventory(player);
        }
        return changed;
    }

    private static boolean moveCoinStackToMainInventory(ServerPlayer player, List<ItemStack> sourceList, int sourceIndex, ItemStack stack) {
        ItemStack moving = stack.copy();
        sourceList.set(sourceIndex, ItemStack.EMPTY);

        int accepted = ScpPickupRouter.acceptCoinStack(player, moving.copy());
        if (accepted >= moving.getCount()) {
            return true;
        }

        int leftover = moving.getCount() - Math.max(0, accepted);
        if (leftover > 0) {
            ItemStack remainder = moving.copy();
            remainder.setCount(leftover);
            sourceList.set(sourceIndex, remainder);
        }
        return accepted > 0;
    }

    private static boolean enforceCoinCap(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int total = countAllCoins(inventory);
        int overflow = total - ScpPickupRouter.MAX_COIN_COUNT;
        if (overflow <= 0) {
            return false;
        }

        int remainingOverflow = overflow;
        remainingOverflow = removeCoinOverflowFromList(inventory.offhand, remainingOverflow);
        remainingOverflow = removeCoinOverflowFromList(inventory.armor, remainingOverflow);
        remainingOverflow = removeCoinOverflowFromRange(inventory.items, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE, remainingOverflow);
        remainingOverflow = removeCoinOverflowFromRange(inventory.items, 35, 8, remainingOverflow);

        ScpPickupRouter.syncVanillaInventory(player);
        showCoinCapMessage(player);
        return remainingOverflow != overflow;
    }

    private static int countAllCoins(Inventory inventory) {
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

    private static int removeCoinOverflowFromList(List<ItemStack> stacks, int overflow) {
        for (int i = 0; i < stacks.size() && overflow > 0; i++) {
            overflow = removeCoinOverflowAt(stacks, i, overflow);
        }
        return overflow;
    }

    private static int removeCoinOverflowFromRange(List<ItemStack> stacks, int startInclusive, int endExclusive, int overflow) {
        int step = startInclusive <= endExclusive ? 1 : -1;
        for (int i = startInclusive; overflow > 0 && i >= 0 && i < stacks.size() && i != endExclusive; i += step) {
            overflow = removeCoinOverflowAt(stacks, i, overflow);
        }
        return overflow;
    }

    private static int removeCoinOverflowAt(List<ItemStack> stacks, int index, int overflow) {
        ItemStack stack = stacks.get(index);
        if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
            return overflow;
        }

        int removed = Math.min(overflow, stack.getCount());
        stack.shrink(removed);
        stacks.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack);
        return overflow - removed;
    }

    private static void showCoinCapMessage(ServerPlayer player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUUID();
        long last = LAST_COIN_MESSAGE_MS.getOrDefault(id, 0L);
        if (now - last < COIN_MESSAGE_COOLDOWN_MS) {
            return;
        }

        LAST_COIN_MESSAGE_MS.put(id, now);
        player.displayClientMessage(Component.literal("You can't carry more coins."), true);
    }

    private static boolean reconcileAccessoryHand(ServerPlayer player, IScpInventory inventory) {
        ItemStack equippedAccessory = inventory.getEquipment(ScpEquipmentSlot.ACCESSORY);
        ItemStack offhand = player.getOffhandItem();

        if (!equippedAccessory.isEmpty() && ScpItemClassifier.isAccessoryHand(equippedAccessory)) {
            if (offhand.isEmpty() || !ItemStack.isSameItemSameTags(normalizeSingle(offhand), normalizeSingle(equippedAccessory))) {
                InventoryActionPacket.syncVanillaEquipmentSlot(player, ScpEquipmentSlot.ACCESSORY, equippedAccessory);
                return true;
            }
            return false;
        }

        if (!offhand.isEmpty() && ScpItemClassifier.isAccessoryHand(offhand) && equippedAccessory.isEmpty()) {
            inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, normalizeSingle(offhand));
            return true;
        }

        return false;
    }

    private static ItemStack normalizeSingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
}
