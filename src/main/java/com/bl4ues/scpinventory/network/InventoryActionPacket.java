package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
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
            if (player == null) return;

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
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static void useSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty()) return;

        ScpItemType type = ScpItemClassifier.getType(stack);
        if (type == ScpItemType.CONSUMABLE) {
            consumeSlot(player, inventory, slot, stack);
            return;
        }

        if (type == ScpItemType.USABLE) {
            useUsableSlot(player, inventory, slot);
        }
    }

    private static void consumeSlot(ServerPlayer player, IScpInventory inventory, int slot, ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        boolean hasVanillaUseResult = stack.isEdible() || animation == UseAnim.EAT || animation == UseAnim.DRINK;
        if (!hasVanillaUseResult) {
            inventory.removeInventoryItem(slot);
            return;
        }

        ItemStack usedStack = stack.copy();
        usedStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(usedStack);

        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                animation == UseAnim.DRINK ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS,
                0.8F,
                0.9F + player.getRandom().nextFloat() * 0.2F
        );

        ItemStack result = usedStack.finishUsingItem(player.level(), player);
        stack.shrink(1);
        inventory.setInventoryItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

        if (!result.isEmpty()) routeUseRemainder(player, inventory, result);
    }

    private static void useUsableSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        int hotbarSlot = findUsableHotbarSlot(player);
        if (hotbarSlot == -1) {
            ModNetwork.showInventoryFull(player);
            return;
        }

        ItemStack usableStack = inventory.extractInventoryItem(slot);
        if (usableStack.isEmpty()) {
            return;
        }

        usableStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(usableStack);
        ScpPickupRouter.markUsableSession(usableStack, player.tickCount);
        boolean continuousUse = usableStack.getUseAnimation() != UseAnim.NONE;

        Inventory vanillaInventory = player.getInventory();
        vanillaInventory.setItem(hotbarSlot, usableStack.copy());
        vanillaInventory.selected = hotbarSlot;
        ScpPickupRouter.syncVanillaInventory(player);
        ModNetwork.activateUsableItem(player, hotbarSlot, continuousUse, usableStack);
    }

    private static int findUsableHotbarSlot(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        if (selected >= VANILLA_HOTBAR_START
                && selected < VANILLA_HOTBAR_END_EXCLUSIVE
                && selected < inventory.items.size()
                && inventory.items.get(selected).isEmpty()) {
            return selected;
        }

        return findFirstEmpty(inventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE);
    }

    private static void routeUseRemainder(ServerPlayer player, IScpInventory inventory, ItemStack remainder) {
        ItemStack leftover = remainder.copy();
        ScpPickupRouter.stripNoMergeMarker(leftover);
        int accepted = ScpPickupRouter.accept(inventory, player, leftover);
        if (accepted > 0) leftover.shrink(accepted);
        if (!leftover.isEmpty()) player.drop(leftover, false);
    }

    private static void equipSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty()) return;

        Optional<ScpEquipmentSlot> equipmentSlot = ScpItemClassifier.getEquipmentSlot(stack);
        if (equipmentSlot.isEmpty()) return;

        ScpEquipmentSlot targetSlot = equipmentSlot.get();
        ItemStack newEquipment = inventory.extractInventoryItem(slot);
        ItemStack previousEquipment = getPreviousEquipmentForReplacement(player, inventory, targetSlot, newEquipment);

        inventory.setEquipment(targetSlot, newEquipment);
        syncVanillaEquipmentSlot(player, targetSlot, newEquipment);

        if (!previousEquipment.isEmpty()) inventory.setInventoryItem(slot, previousEquipment);
    }

    private static ItemStack getPreviousEquipmentForReplacement(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot targetSlot, ItemStack incomingStack) {
        ItemStack previousEquipment = inventory.getEquipment(targetSlot);
        if (targetSlot != ScpEquipmentSlot.ACCESSORY || !previousEquipment.isEmpty() || ScpItemClassifier.isAccessoryHand(incomingStack)) {
            return previousEquipment;
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && ScpItemClassifier.isAccessoryHand(offhand)) {
            ItemStack copy = offhand.copy();
            copy.setCount(1);
            return copy;
        }

        return previousEquipment;
    }

    public static void syncVanillaEquipmentSlot(ServerPlayer player, ScpEquipmentSlot slot, ItemStack stack) {
        if (slot == ScpEquipmentSlot.ACCESSORY) {
            syncAccessorySlot(player, stack);
            return;
        }

        EquipmentSlot vanillaSlot = getVanillaEquipmentSlot(slot);
        if (vanillaSlot != null) {
            player.setItemSlot(vanillaSlot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            return;
        }

        if (slot == ScpEquipmentSlot.WEAPON) syncMainInventoryMirror(player, slot, stack);
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

    private static void syncAccessorySlot(ServerPlayer player, ItemStack stack) {
        if (player == null) return;

        Inventory inventory = player.getInventory();
        removeAllMirrorsForSlot(inventory, ScpEquipmentSlot.ACCESSORY);

        if (stack == null || stack.isEmpty()) {
            clearOffhandAccessory(player);
            ScpPickupRouter.syncVanillaInventory(player);
            return;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        if (ScpItemClassifier.isAccessoryHand(normalized)) {
            preserveCurrentOffhand(player, normalized);
            player.setItemSlot(EquipmentSlot.OFFHAND, normalized);
            ScpPickupRouter.syncVanillaInventory(player);
            return;
        }

        clearOffhandAccessory(player);
        syncMainInventoryMirror(player, ScpEquipmentSlot.ACCESSORY, normalized);
        ScpPickupRouter.syncVanillaInventory(player);
    }

    private static void clearOffhandAccessory(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        if (ScpItemClassifier.isAccessoryHand(offhand)) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void preserveCurrentOffhand(ServerPlayer player, ItemStack incoming) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) {
            return;
        }

        if (ItemStack.isSameItemSameTags(offhand, incoming)) {
            if (offhand.getCount() > incoming.getCount()) {
                ItemStack extra = offhand.copy();
                extra.setCount(offhand.getCount() - incoming.getCount());
                storeOrDropVanillaRemainder(player, extra);
            }
            return;
        }

        storeOrDropVanillaRemainder(player, offhand.copy());
    }

    private static void storeOrDropVanillaRemainder(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
        ScpPickupRouter.syncVanillaInventory(player);
    }

    private static void syncMainInventoryMirror(ServerPlayer player, ScpEquipmentSlot slot, ItemStack stack) {
        if (player == null || slot == null) return;

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
            if (candidate.isEmpty() || ScpItemClassifier.getEquipmentSlot(candidate).orElse(null) != slot) continue;

            ItemStack normalized = candidate.copy();
            normalized.setCount(1);
            if (!ItemStack.isSameItemSameTags(normalized, expected)) inventory.items.set(i, ItemStack.EMPTY);
        }
    }

    private static void removeAllMirrorsForSlot(Inventory inventory, ScpEquipmentSlot slot) {
        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (!candidate.isEmpty() && ScpItemClassifier.getEquipmentSlot(candidate).orElse(null) == slot) inventory.items.set(i, ItemStack.EMPTY);
        }
    }

    private static int findExactMirror(Inventory inventory, ScpEquipmentSlot slot, ItemStack expected) {
        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (candidate.isEmpty() || ScpItemClassifier.getEquipmentSlot(candidate).orElse(null) != slot) continue;

            ItemStack normalized = candidate.copy();
            normalized.setCount(1);
            if (ItemStack.isSameItemSameTags(normalized, expected)) return i;
        }

        return -1;
    }

    private static int findPreferredEmptySlot(Inventory inventory, ScpEquipmentSlot slot) {
        int preferred = slot == ScpEquipmentSlot.WEAPON
                ? findFirstEmpty(inventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE)
                : findFirstEmpty(inventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE);

        if (preferred != -1) return preferred;

        return slot == ScpEquipmentSlot.WEAPON
                ? findFirstEmpty(inventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE)
                : findFirstEmpty(inventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE);
    }

    private static int findFirstEmpty(Inventory inventory, int start, int endExclusive) {
        for (int i = start; i < endExclusive && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) return i;
        }
        return -1;
    }
}
