package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class ScpInventoryMaintenanceEvents {

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;

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

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }

        ItemStack tossedStack = event.getEntity().getItem();
        if (!ScpPickupRouter.isCoinMirror(tossedStack)) {
            return;
        }

        ScpPickupRouter.stripCoinMirror(tossedStack);
        event.getEntity().setItem(tossedStack);
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
                changed |= ScpPickupRouter.reconcileCoinMirrors(player, inventory);
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

            if (vanillaInventory.selected == slot) {
                continue;
            }

            changed |= returnUsableSessionToCustomInventory(player, inventory, vanillaInventory, slot, stack);
        }

        return changed;
    }

    private static boolean returnUsableSessionToCustomInventory(ServerPlayer player, IScpInventory inventory, Inventory vanillaInventory, int hotbarSlot, ItemStack stack) {
        ItemStack returning = stack.copy();
        ScpPickupRouter.stripUsableSession(returning);
        ScpPickupRouter.stripNoMergeMarker(returning);

        if (ScpPickupRouter.isCoinMirror(returning) || ScpItemClassifier.isCoin(returning)) {
            ScpPickupRouter.stripUsableSession(stack);
            vanillaInventory.setItem(hotbarSlot, stack);
            ScpPickupRouter.syncVanillaInventory(player);
            return true;
        }

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

        return false;
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
