package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class ScpInventoryMaintenanceEvents {

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;

    private ScpInventoryMaintenanceEvents() {
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

        event.setCanceled(true);
        ScpPickupRouter.acceptCoinStack(player, tossedStack.copy());
        event.getEntity().discard();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            boolean changed = migrateCoinsFromCustomInventory(player, inventory);
            changed |= moveCoinsOutOfInvalidVanillaSlots(player);
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
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    private static boolean moveCoinStackToMainInventory(ServerPlayer player, java.util.List<ItemStack> sourceList, int sourceIndex, ItemStack stack) {
        int accepted = ScpPickupRouter.acceptCoinStack(player, stack.copy());
        if (accepted <= 0) {
            return false;
        }

        if (accepted >= stack.getCount()) {
            sourceList.set(sourceIndex, ItemStack.EMPTY);
        } else {
            stack.shrink(accepted);
            sourceList.set(sourceIndex, stack);
        }
        return true;
    }

    private static boolean reconcileAccessoryHand(ServerPlayer player, IScpInventory inventory) {
        ItemStack equippedAccessory = inventory.getEquipment(ScpEquipmentSlot.ACCESSORY);
        ItemStack offhand = player.getOffhandItem();

        if (!offhand.isEmpty() && ScpItemClassifier.isAccessoryHand(offhand)) {
            if (!equippedAccessory.isEmpty() && !ScpItemClassifier.isAccessoryHand(equippedAccessory)) {
                return false;
            }

            ItemStack normalizedOffhand = offhand.copy();
            normalizedOffhand.setCount(1);
            if (equippedAccessory.isEmpty()
                    || equippedAccessory.getCount() != 1
                    || !ItemStack.isSameItemSameTags(normalizedOffhand, equippedAccessory)) {
                inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, normalizedOffhand);
                return true;
            }

            return false;
        }

        if (!equippedAccessory.isEmpty() && ScpItemClassifier.isAccessoryHand(equippedAccessory)) {
            inventory.clearEquipment(ScpEquipmentSlot.ACCESSORY);
            return true;
        }

        return false;
    }
}
