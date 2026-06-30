package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class PendingConsumableUseManager {

    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END_EXCLUSIVE = 9;
    private static final Map<UUID, PendingUse> PENDING_USES = new HashMap<>();

    private PendingConsumableUseManager() {
    }

    public static boolean start(ServerPlayer player, IScpInventory scpInventory, int sourceSlot, ItemStack sourceStack) {
        if (player == null || scpInventory == null || sourceStack == null || sourceStack.isEmpty()) {
            return false;
        }
        if (PENDING_USES.containsKey(player.getUUID())) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        int visualSlot = findEmptyHotbarSlot(vanillaInventory);
        if (visualSlot == -1) {
            return false;
        }

        ItemStack currentSource = scpInventory.getInventoryItem(sourceSlot);
        if (currentSource.isEmpty() || !ItemStack.isSameItemSameTags(currentSource, sourceStack)) {
            return false;
        }

        ItemStack visualStack = sourceStack.copy();
        visualStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(visualStack);

        currentSource.shrink(1);
        scpInventory.setInventoryItem(sourceSlot, currentSource.isEmpty() ? ItemStack.EMPTY : currentSource);

        int previousSelected = vanillaInventory.selected;
        vanillaInventory.items.set(visualSlot, visualStack.copy());
        vanillaInventory.selected = visualSlot;
        vanillaInventory.setChanged();
        player.containerMenu.broadcastChanges();
        player.connection.send(new ClientboundSetCarriedItemPacket(visualSlot));

        int duration = Math.max(1, visualStack.getUseDuration());
        PENDING_USES.put(player.getUUID(), new PendingUse(visualSlot, previousSelected, player.tickCount, duration, visualStack.copy()));

        player.startUsingItem(InteractionHand.MAIN_HAND);
        ModNetwork.syncTo(player, scpInventory);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        PendingUse pending = PENDING_USES.get(player.getUUID());
        if (pending == null) {
            return;
        }

        int elapsed = player.tickCount - pending.startedAtTick();
        Inventory vanillaInventory = player.getInventory();
        ItemStack handStack = vanillaInventory.items.get(pending.hotbarSlot());

        if (elapsed < pending.useDurationTicks()) {
            if (!handStack.isEmpty() && !player.isUsingItem()) {
                vanillaInventory.selected = pending.hotbarSlot();
                player.connection.send(new ClientboundSetCarriedItemPacket(pending.hotbarSlot()));
                player.startUsingItem(InteractionHand.MAIN_HAND);
            }
            return;
        }

        if (!handStack.isEmpty() && ItemStack.isSameItemSameTags(handStack, pending.originalStack())) {
            ItemStack result = handStack.copy().finishUsingItem(player.level(), player);
            vanillaInventory.items.set(pending.hotbarSlot(), result);
            player.stopUsingItem();
            vanillaInventory.setChanged();
            handStack = result;
        }

        final ItemStack remainder = handStack.copy();
        vanillaInventory.items.set(pending.hotbarSlot(), ItemStack.EMPTY);
        vanillaInventory.selected = pending.previousSelectedSlot();
        vanillaInventory.setChanged();
        player.containerMenu.broadcastChanges();
        player.connection.send(new ClientboundSetCarriedItemPacket(pending.previousSelectedSlot()));
        PENDING_USES.remove(player.getUUID());

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(scpInventory -> {
            routeRemainder(player, scpInventory, remainder);
            ModNetwork.syncTo(player, scpInventory);
        });
    }

    private static int findEmptyHotbarSlot(Inventory inventory) {
        if (inventory == null) {
            return -1;
        }

        int selected = inventory.selected;
        if (selected >= HOTBAR_START && selected < HOTBAR_END_EXCLUSIVE && inventory.items.get(selected).isEmpty()) {
            return selected;
        }

        for (int i = HOTBAR_START; i < HOTBAR_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static void routeRemainder(ServerPlayer player, IScpInventory inventory, ItemStack remainder) {
        if (remainder == null || remainder.isEmpty()) {
            return;
        }

        ScpPickupRouter.stripNoMergeMarker(remainder);
        int accepted = ScpPickupRouter.accept(inventory, player, remainder);
        if (accepted > 0) {
            remainder.shrink(accepted);
        }
        if (!remainder.isEmpty()) {
            player.drop(remainder, false);
        }
    }

    private record PendingUse(int hotbarSlot, int previousSelectedSlot, int startedAtTick, int useDurationTicks, ItemStack originalStack) {
    }
}
