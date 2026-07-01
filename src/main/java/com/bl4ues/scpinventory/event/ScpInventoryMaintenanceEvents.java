package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class ScpInventoryMaintenanceEvents {

    private static final int SERVER_SYNC_INTERVAL_TICKS = 20;

    private ScpInventoryMaintenanceEvents() {
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack tossedStack = event.getEntity().getItem();
        if (!ScpItemClassifier.isCoin(tossedStack)) {
            return;
        }

        event.setCanceled(true);
        ScpPickupRouter.acceptCoinStack(player, tossedStack.copy());
        event.getEntity().discard();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % SERVER_SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            boolean changed = migrateCoinsFromCustomInventory(player, inventory);
            changed |= reconcileAccessoryHand(player, inventory);
            if (changed) {
                ModNetwork.syncTo(player, inventory);
            }
        });
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
            }
        }
        return changed;
    }

    private static boolean reconcileAccessoryHand(ServerPlayer player, IScpInventory inventory) {
        ItemStack equippedAccessory = inventory.getEquipment(ScpEquipmentSlot.ACCESSORY);
        ItemStack offhand = player.getOffhandItem();

        if (equippedAccessory.isEmpty()) {
            if (!offhand.isEmpty() && ScpItemClassifier.isAccessoryHand(offhand)) {
                ItemStack copy = offhand.copy();
                copy.setCount(1);
                inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, copy);
                return true;
            }
            return false;
        }

        if (!ScpItemClassifier.isAccessoryHand(equippedAccessory)) {
            return false;
        }

        if (offhand.isEmpty() || !ItemStack.isSameItem(offhand, equippedAccessory)) {
            inventory.clearEquipment(ScpEquipmentSlot.ACCESSORY);
            return true;
        }

        ItemStack normalizedOffhand = offhand.copy();
        normalizedOffhand.setCount(1);
        if (!ItemStack.isSameItemSameTags(normalizedOffhand, equippedAccessory) || equippedAccessory.getCount() != 1) {
            inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, normalizedOffhand);
            return true;
        }

        return false;
    }
}
