package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public class VanillaMirrorSyncHandler {

    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;
    private static final int SYNC_INTERVAL_TICKS = 5;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            boolean changed = false;
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.HEAD, EquipmentSlot.HEAD);
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.BODY, EquipmentSlot.CHEST);
            changed |= syncKeys(player, inventory);

            if (changed) {
                ModNetwork.syncTo(player, inventory);
            }
        });
    }

    private static boolean syncEquipmentSlot(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot customSlot, EquipmentSlot vanillaSlot) {
        ItemStack vanillaStack = player.getItemBySlot(vanillaSlot);
        ItemStack customStack = inventory.getEquipment(customSlot);

        if (vanillaStack.isEmpty()) {
            if (!customStack.isEmpty()) {
                inventory.clearEquipment(customSlot);
                return true;
            }
            return false;
        }

        if (ScpItemClassifier.getEquipmentSlot(vanillaStack).orElse(null) != customSlot) {
            if (!customStack.isEmpty()) {
                inventory.clearEquipment(customSlot);
                return true;
            }
            return false;
        }

        ItemStack normalized = vanillaStack.copy();
        normalized.setCount(1);

        if (!ItemStack.isSameItemSameTags(customStack, normalized) || customStack.getCount() != 1) {
            inventory.setEquipment(customSlot, normalized);
            InventoryActionPacket.syncVanillaEquipmentSlot(player, customSlot, normalized);
            return true;
        }

        return false;
    }

    private static boolean syncKeys(ServerPlayer player, IScpInventory inventory) {
        List<ItemStack> vanillaKeys = getVanillaKeys(player);
        List<ItemStack> customKeys = inventory.getKeys();

        if (sameKeyList(vanillaKeys, customKeys)) {
            return false;
        }

        inventory.setKeys(vanillaKeys);
        return true;
    }

    private static List<ItemStack> getVanillaKeys(ServerPlayer player) {
        List<ItemStack> keys = new ArrayList<>();
        Inventory vanillaInventory = player.getInventory();

        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < vanillaInventory.items.size(); i++) {
            ItemStack stack = vanillaInventory.items.get(i);
            if (!stack.isEmpty() && ScpItemClassifier.getType(stack) == ScpItemType.KEY) {
                int amount = Math.min(stack.getCount(), IScpInventory.MAX_KEY_COUNT - keys.size());
                for (int j = 0; j < amount; j++) {
                    ItemStack singleKey = stack.copy();
                    singleKey.setCount(1);
                    keys.add(singleKey);
                }
            }

            if (keys.size() >= IScpInventory.MAX_KEY_COUNT) {
                break;
            }
        }

        return keys;
    }

    private static boolean sameKeyList(List<ItemStack> vanillaKeys, List<ItemStack> customKeys) {
        if (vanillaKeys.size() != customKeys.size()) {
            return false;
        }

        for (int i = 0; i < vanillaKeys.size(); i++) {
            ItemStack left = vanillaKeys.get(i);
            ItemStack right = customKeys.get(i);
            if (!ItemStack.isSameItemSameTags(left, right)) {
                return false;
            }
        }

        return true;
    }
}
