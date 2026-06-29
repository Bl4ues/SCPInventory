package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public class VanillaMirrorSyncHandler {

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;
    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;
    private static final int SYNC_INTERVAL_TICKS = 1;
    private static final long FULL_MESSAGE_COOLDOWN_MS = 550L;
    private static final Map<UUID, Long> LAST_FULL_MESSAGE = new HashMap<>();

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
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.CHEST, EquipmentSlot.CHEST);
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.LEGS, EquipmentSlot.LEGS);
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.FEET, EquipmentSlot.FEET);
            changed |= routeVanillaInventoryToCustom(player, inventory);
            changed |= ensureEquippedMainMirror(player, inventory, ScpEquipmentSlot.WEAPON, true);
            changed |= ensureEquippedMainMirror(player, inventory, ScpEquipmentSlot.ACCESSORY, false);
            changed |= syncKeys(player, inventory);

            if (changed) {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
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

    private static boolean routeVanillaInventoryToCustom(ServerPlayer player, IScpInventory inventory) {
        boolean changed = false;
        Inventory vanillaInventory = player.getInventory();

        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < vanillaInventory.items.size(); i++) {
            ItemStack stack = vanillaInventory.items.get(i);
            if (stack.isEmpty()) {
                continue;
            }

            ScpItemType type = ScpItemClassifier.getType(stack);
            if (type == ScpItemType.KEY) {
                continue;
            }

            ScpEquipmentSlot preservedSlot = getPreservedMirrorSlot(type);
            if (preservedSlot != null && syncPreservedEquipmentMirror(inventory, preservedSlot, stack)) {
                changed = true;
                changed |= routeMirrorOverflow(player, inventory, vanillaInventory, i, stack);
                continue;
            }

            int originalCount = stack.getCount();
            int accepted = ScpPickupRouter.accept(inventory, player, stack);
            if (accepted <= 0) {
                showInventoryFullThrottled(player);
                continue;
            }

            changed = true;
            if (accepted >= originalCount) {
                vanillaInventory.items.set(i, ItemStack.EMPTY);
            } else {
                stack.shrink(accepted);
                vanillaInventory.items.set(i, stack);
                showInventoryFullThrottled(player);
            }
        }

        return changed;
    }

    private static boolean routeMirrorOverflow(ServerPlayer player, IScpInventory inventory, Inventory vanillaInventory, int slot, ItemStack stack) {
        if (stack.getCount() <= 1) {
            return false;
        }

        ItemStack overflow = stack.copy();
        overflow.setCount(stack.getCount() - 1);
        int accepted = ScpPickupRouter.accept(inventory, player, overflow);
        if (accepted <= 0) {
            showInventoryFullThrottled(player);
            return false;
        }

        int remainingOverflow = overflow.getCount() - accepted;
        stack.setCount(1 + Math.max(0, remainingOverflow));
        vanillaInventory.items.set(slot, stack);

        if (remainingOverflow > 0) {
            showInventoryFullThrottled(player);
        }

        return true;
    }

    private static ScpEquipmentSlot getPreservedMirrorSlot(ScpItemType type) {
        return switch (type) {
            case WEAPON -> ScpEquipmentSlot.WEAPON;
            case ACCESSORY -> ScpEquipmentSlot.ACCESSORY;
            default -> null;
        };
    }

    private static boolean syncPreservedEquipmentMirror(IScpInventory inventory, ScpEquipmentSlot slot, ItemStack vanillaStack) {
        ItemStack equipped = inventory.getEquipment(slot);
        if (equipped.isEmpty()) {
            return false;
        }

        ItemStack normalized = vanillaStack.copy();
        normalized.setCount(1);
        if (!ItemStack.isSameItemSameTags(equipped, normalized)) {
            return false;
        }

        if (equipped.getCount() != 1 || !ItemStack.isSameItemSameTags(equipped, normalized)) {
            inventory.setEquipment(slot, normalized);
            return true;
        }

        return false;
    }

    private static boolean ensureEquippedMainMirror(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot slot, boolean preferHotbar) {
        ItemStack equipped = inventory.getEquipment(slot);
        if (equipped.isEmpty()) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        if (findSameStack(vanillaInventory, equipped) != -1) {
            return false;
        }

        int targetSlot = preferHotbar
                ? findFirstEmpty(vanillaInventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE)
                : findFirstEmpty(vanillaInventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE);

        if (targetSlot == -1) {
            targetSlot = preferHotbar
                    ? findFirstEmpty(vanillaInventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE)
                    : findFirstEmpty(vanillaInventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE);
        }

        if (targetSlot == -1) {
            return false;
        }

        ItemStack mirror = equipped.copy();
        mirror.setCount(1);
        vanillaInventory.items.set(targetSlot, mirror);
        return true;
    }

    private static int findSameStack(Inventory inventory, ItemStack stack) {
        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameTags(candidate, stack)) {
                return i;
            }
        }
        return -1;
    }

    private static int findFirstEmpty(Inventory inventory, int start, int endExclusive) {
        for (int i = start; i < endExclusive && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
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

        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < vanillaInventory.items.size(); i++) {
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

    private static void showInventoryFullThrottled(ServerPlayer player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();
        long lastShown = LAST_FULL_MESSAGE.getOrDefault(playerId, 0L);

        if (now - lastShown >= FULL_MESSAGE_COOLDOWN_MS) {
            LAST_FULL_MESSAGE.put(playerId, now);
            ModNetwork.showInventoryFull(player);
        }
    }
}
