package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class InventoryActionPacket {

    public static final String ACTION_DROP = "DROP";
    public static final String ACTION_USE = "USE";
    public static final String ACTION_EQUIP = "EQUIP";

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;
    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;

    private final int slot;
    private final String action;

    public InventoryActionPacket(int slot, String action) {
        this.slot = slot;
        this.action = action == null ? "" : action;
    }

    public static void encode(InventoryActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
        buf.writeUtf(msg.action);
    }

    public static InventoryActionPacket decode(FriendlyByteBuf buf) {
        return new InventoryActionPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(InventoryActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (!inventory.isValidMainSlot(msg.slot)) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                switch (msg.action) {
                    case ACTION_DROP -> moveSlotToWorld(player, inventory, msg.slot);
                    case ACTION_USE -> useSlot(player, inventory, msg.slot);
                    case ACTION_EQUIP -> equipSlot(player, inventory, msg.slot);
                    default -> {
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void moveSlotToWorld(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.extractInventoryItem(slot);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static void useSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty() || ScpItemClassifier.getType(stack) != ScpItemType.CONSUMABLE) {
            return;
        }

        if (stack.isEdible()) {
            ItemStack result = player.eat(player.level(), stack.copy());
            inventory.setInventoryItem(slot, result);
        } else {
            inventory.removeInventoryItem(slot);
        }
    }

    private static void equipSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty()) {
            return;
        }

        Optional<ScpEquipmentSlot> equipmentSlot = ScpItemClassifier.getEquipmentSlot(stack);
        if (equipmentSlot.isEmpty()) {
            return;
        }

        ScpEquipmentSlot targetSlot = equipmentSlot.get();
        ItemStack newEquipment = inventory.extractInventoryItem(slot);
        ItemStack previousEquipment = inventory.getEquipment(targetSlot);

        inventory.setEquipment(targetSlot, newEquipment);
        syncVanillaEquipmentSlot(player, targetSlot, newEquipment);

        if (!previousEquipment.isEmpty()) {
            inventory.setInventoryItem(slot, previousEquipment);
        }
    }

    public static void syncVanillaEquipmentSlot(ServerPlayer player, ScpEquipmentSlot slot, ItemStack stack) {
        EquipmentSlot vanillaSlot = getVanillaEquipmentSlot(slot);
        if (vanillaSlot != null) {
            player.setItemSlot(vanillaSlot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            return;
        }

        if (slot == ScpEquipmentSlot.WEAPON || slot == ScpEquipmentSlot.ACCESSORY) {
            syncMainInventoryMirror(player, slot, stack);
        }
    }

    public static EquipmentSlot getVanillaEquipmentSlot(ScpEquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static void syncMainInventoryMirror(ServerPlayer player, ScpEquipmentSlot slot, ItemStack stack) {
        if (player == null || slot == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (stack == null || stack.isEmpty()) {
            removeAllMirrorsForSlot(inventory, slot);
            inventory.setChanged();
            return;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        removeMismatchedMirrors(inventory, slot, normalized);

        int existingSlot = findExactMirror(inventory, slot, normalized);
        if (existingSlot != -1) {
            ItemStack existing = inventory.items.get(existingSlot);
            if (existing.getCount() != 1) {
                inventory.items.set(existingSlot, normalized);
                inventory.setChanged();
            }
            return;
        }

        int targetSlot = findPreferredEmptySlot(inventory, slot);
        if (targetSlot != -1) {
            inventory.items.set(targetSlot, normalized);
            inventory.setChanged();
        }
    }

    private static void removeMismatchedMirrors(Inventory inventory, ScpEquipmentSlot slot, ItemStack expected) {
        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (candidate.isEmpty() || ScpItemClassifier.getEquipmentSlot(candidate).orElse(null) != slot) {
                continue;
            }

            ItemStack normalized = candidate.copy();
            normalized.setCount(1);
            if (!ItemStack.isSameItemSameTags(normalized, expected)) {
                inventory.items.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static void removeAllMirrorsForSlot(Inventory inventory, ScpEquipmentSlot slot) {
        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (!candidate.isEmpty() && ScpItemClassifier.getEquipmentSlot(candidate).orElse(null) == slot) {
                inventory.items.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static int findExactMirror(Inventory inventory, ScpEquipmentSlot slot, ItemStack expected) {
        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (candidate.isEmpty() || ScpItemClassifier.getEquipmentSlot(candidate).orElse(null) != slot) {
                continue;
            }

            ItemStack normalized = candidate.copy();
            normalized.setCount(1);
            if (ItemStack.isSameItemSameTags(normalized, expected)) {
                return i;
            }
        }

        return -1;
    }

    private static int findPreferredEmptySlot(Inventory inventory, ScpEquipmentSlot slot) {
        int preferred = slot == ScpEquipmentSlot.WEAPON
                ? findFirstEmpty(inventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE)
                : findFirstEmpty(inventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE);

        if (preferred != -1) {
            return preferred;
        }

        return slot == ScpEquipmentSlot.WEAPON
                ? findFirstEmpty(inventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE)
                : findFirstEmpty(inventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE);
    }

    private static int findFirstEmpty(Inventory inventory, int start, int endExclusive) {
        for (int i = start; i < endExclusive && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
