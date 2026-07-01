package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class InventoryMovePacket {

    public static final String PLACE_MAIN = "MAIN";
    public static final String PLACE_EQUIPMENT = "EQUIPMENT";
    public static final String PLACE_WORLD = "WORLD";

    private final String sourcePlace;
    private final int sourceIndex;
    private final String sourceSlot;
    private final String targetPlace;
    private final int targetIndex;
    private final String targetSlot;

    public InventoryMovePacket(String sourcePlace, int sourceIndex, String sourceSlot, String targetPlace, int targetIndex, String targetSlot) {
        this.sourcePlace = sourcePlace == null ? "" : sourcePlace;
        this.sourceIndex = sourceIndex;
        this.sourceSlot = sourceSlot == null ? "" : sourceSlot;
        this.targetPlace = targetPlace == null ? "" : targetPlace;
        this.targetIndex = targetIndex;
        this.targetSlot = targetSlot == null ? "" : targetSlot;
    }

    public static void encode(InventoryMovePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sourcePlace);
        buf.writeInt(msg.sourceIndex);
        buf.writeUtf(msg.sourceSlot);
        buf.writeUtf(msg.targetPlace);
        buf.writeInt(msg.targetIndex);
        buf.writeUtf(msg.targetSlot);
    }

    public static InventoryMovePacket decode(FriendlyByteBuf buf) {
        return new InventoryMovePacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(InventoryMovePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                handleMove(player, inventory, msg);
                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleMove(ServerPlayer player, IScpInventory inventory, InventoryMovePacket msg) {
        if (PLACE_MAIN.equals(msg.sourcePlace)) {
            if (PLACE_WORLD.equals(msg.targetPlace)) {
                moveMainToWorld(player, inventory, msg.sourceIndex);
                return;
            }

            if (PLACE_MAIN.equals(msg.targetPlace)) {
                moveMainToMain(inventory, msg.sourceIndex, msg.targetIndex);
                return;
            }

            if (PLACE_EQUIPMENT.equals(msg.targetPlace)) {
                ScpEquipmentSlot targetSlot = parseSlot(msg.targetSlot);
                if (targetSlot != null) {
                    moveMainToEquipment(player, inventory, msg.sourceIndex, targetSlot);
                }
            }
            return;
        }

        if (PLACE_EQUIPMENT.equals(msg.sourcePlace)) {
            ScpEquipmentSlot sourceSlot = parseSlot(msg.sourceSlot);
            if (sourceSlot == null) {
                return;
            }

            if (PLACE_WORLD.equals(msg.targetPlace)) {
                moveEquipmentToWorld(player, inventory, sourceSlot);
                return;
            }

            if (PLACE_MAIN.equals(msg.targetPlace)) {
                moveEquipmentToMain(player, inventory, sourceSlot, msg.targetIndex);
                return;
            }

            if (PLACE_EQUIPMENT.equals(msg.targetPlace)) {
                ScpEquipmentSlot targetSlot = parseSlot(msg.targetSlot);
                if (targetSlot != null) {
                    moveEquipmentToEquipment(player, inventory, sourceSlot, targetSlot);
                }
            }
        }
    }

    private static void moveMainToWorld(ServerPlayer player, IScpInventory inventory, int sourceIndex) {
        if (!inventory.isValidMainSlot(sourceIndex)) {
            return;
        }

        ItemStack stack = inventory.extractInventoryItem(sourceIndex);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static void moveMainToMain(IScpInventory inventory, int sourceIndex, int targetIndex) {
        if (!inventory.isValidMainSlot(sourceIndex) || !inventory.isValidMainSlot(targetIndex) || sourceIndex == targetIndex) {
            return;
        }

        ItemStack sourceStack = inventory.getInventoryItem(sourceIndex);
        if (sourceStack.isEmpty()) {
            return;
        }

        ItemStack targetStack = inventory.getInventoryItem(targetIndex);
        inventory.setInventoryItem(targetIndex, sourceStack);
        inventory.setInventoryItem(sourceIndex, targetStack);
    }

    private static void moveMainToEquipment(ServerPlayer player, IScpInventory inventory, int sourceIndex, ScpEquipmentSlot targetSlot) {
        if (!inventory.isValidMainSlot(sourceIndex)) {
            return;
        }

        ItemStack stack = inventory.getInventoryItem(sourceIndex);
        if (stack.isEmpty()) {
            return;
        }

        Optional<ScpEquipmentSlot> classifiedSlot = ScpItemClassifier.getEquipmentSlot(stack);
        if (classifiedSlot.isEmpty() || classifiedSlot.get() != targetSlot) {
            return;
        }

        ItemStack movingStack = inventory.extractInventoryItem(sourceIndex);
        ItemStack previousEquipment = getPreviousEquipmentForReplacement(player, inventory, targetSlot, movingStack);

        inventory.setEquipment(targetSlot, movingStack);
        InventoryActionPacket.syncVanillaEquipmentSlot(player, targetSlot, movingStack);

        if (!previousEquipment.isEmpty()) {
            inventory.setInventoryItem(sourceIndex, previousEquipment);
        }
    }

    private static ItemStack getPreviousEquipmentForReplacement(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot targetSlot, ItemStack incomingStack) {
        ItemStack previousEquipment = getEffectiveEquipment(player, inventory, targetSlot);
        if (targetSlot != ScpEquipmentSlot.ACCESSORY || !previousEquipment.isEmpty() || ScpItemClassifier.isAccessoryHand(incomingStack)) {
            return previousEquipment;
        }

        return previousEquipment;
    }

    private static void moveEquipmentToWorld(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot sourceSlot) {
        ItemStack stack = getEffectiveEquipment(player, inventory, sourceSlot);
        if (!stack.isEmpty()) {
            inventory.clearEquipment(sourceSlot);
            InventoryActionPacket.syncVanillaEquipmentSlot(player, sourceSlot, ItemStack.EMPTY);
            player.drop(stack, false);
        }
    }

    private static void moveEquipmentToMain(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot sourceSlot, int targetIndex) {
        ItemStack equippedStack = getEffectiveEquipment(player, inventory, sourceSlot);
        if (equippedStack.isEmpty()) {
            return;
        }

        if (targetIndex < 0) {
            addEquipmentToFirstAvailableSlot(player, inventory, sourceSlot, equippedStack);
            return;
        }

        if (!inventory.isValidMainSlot(targetIndex)) {
            addEquipmentToFirstAvailableSlot(player, inventory, sourceSlot, equippedStack);
            return;
        }

        ItemStack targetStack = inventory.getInventoryItem(targetIndex);
        if (targetStack.isEmpty()) {
            inventory.setInventoryItem(targetIndex, equippedStack);
            inventory.clearEquipment(sourceSlot);
            InventoryActionPacket.syncVanillaEquipmentSlot(player, sourceSlot, ItemStack.EMPTY);
            return;
        }

        Optional<ScpEquipmentSlot> targetSlot = ScpItemClassifier.getEquipmentSlot(targetStack);
        if (targetSlot.isPresent() && targetSlot.get() == sourceSlot) {
            inventory.setEquipment(sourceSlot, targetStack);
            inventory.setInventoryItem(targetIndex, equippedStack);
            InventoryActionPacket.syncVanillaEquipmentSlot(player, sourceSlot, targetStack);
            return;
        }

        addEquipmentToFirstAvailableSlot(player, inventory, sourceSlot, equippedStack);
    }

    private static void addEquipmentToFirstAvailableSlot(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot sourceSlot, ItemStack equippedStack) {
        if (inventory.addInventoryItem(equippedStack)) {
            inventory.clearEquipment(sourceSlot);
            InventoryActionPacket.syncVanillaEquipmentSlot(player, sourceSlot, ItemStack.EMPTY);
        } else {
            ModNetwork.showInventoryFull(player);
        }
    }

    private static void moveEquipmentToEquipment(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot sourceSlot, ScpEquipmentSlot targetSlot) {
        if (sourceSlot == targetSlot) {
            return;
        }

        ItemStack sourceStack = getEffectiveEquipment(player, inventory, sourceSlot);
        if (sourceStack.isEmpty()) {
            return;
        }

        Optional<ScpEquipmentSlot> classifiedSlot = ScpItemClassifier.getEquipmentSlot(sourceStack);
        if (classifiedSlot.isEmpty() || classifiedSlot.get() != targetSlot) {
            return;
        }

        ItemStack targetStack = getEffectiveEquipment(player, inventory, targetSlot);
        if (!targetStack.isEmpty()) {
            Optional<ScpEquipmentSlot> targetClassifiedSlot = ScpItemClassifier.getEquipmentSlot(targetStack);
            if (targetClassifiedSlot.isEmpty() || targetClassifiedSlot.get() != sourceSlot) {
                return;
            }
        }

        inventory.setEquipment(targetSlot, sourceStack);
        inventory.setEquipment(sourceSlot, targetStack);
        InventoryActionPacket.syncVanillaEquipmentSlot(player, sourceSlot, targetStack);
        InventoryActionPacket.syncVanillaEquipmentSlot(player, targetSlot, sourceStack);
    }

    private static ItemStack getEffectiveEquipment(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot slot) {
        ItemStack stack = inventory.getEquipment(slot);
        if (!stack.isEmpty()) {
            return stack;
        }

        if (slot == ScpEquipmentSlot.ACCESSORY && player != null && ScpItemClassifier.isAccessoryHand(player.getOffhandItem())) {
            ItemStack offhand = player.getOffhandItem().copy();
            offhand.setCount(1);
            inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, offhand.copy());
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private static ScpEquipmentSlot parseSlot(String name) {
        return ScpEquipmentSlot.fromName(name).orElse(null);
    }
}
