package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class UsableItemHoldClient {

    private static final int MAX_HOLD_TICKS = 120;
    private static final int STARTUP_GRACE_TICKS = 24;
    private static final int MAX_SYNC_WAIT_TICKS = 20;
    private static final int CLICK_ONLY_TICKS = 2;

    private static int targetHotbarSlot = -1;
    private static boolean continuousUse = false;
    private static int syncWaitTicks = 0;
    private static int ticksRemaining = 0;
    private static int ticksElapsed = 0;
    private static boolean activated = false;
    private static boolean sawUsingItem = false;

    private UsableItemHoldClient() {
    }

    public static void start(int hotbarSlot, boolean continuous) {
        if (hotbarSlot < 0 || hotbarSlot >= 9) {
            stop(Minecraft.getInstance());
            return;
        }

        targetHotbarSlot = hotbarSlot;
        continuousUse = continuous;
        syncWaitTicks = MAX_SYNC_WAIT_TICKS;
        ticksRemaining = continuous ? MAX_HOLD_TICKS : CLICK_ONLY_TICKS;
        ticksElapsed = 0;
        activated = false;
        sawUsingItem = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || targetHotbarSlot < 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stop(mc);
            return;
        }

        if (!activated) {
            if (!isHotbarSlotReady(player)) {
                syncWaitTicks--;
                if (syncWaitTicks <= 0) {
                    stop(mc);
                }
                return;
            }

            player.getInventory().selected = targetHotbarSlot;
            if (mc.gameMode != null) {
                mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            }
            player.swing(InteractionHand.MAIN_HAND);
            activated = true;
        }

        ticksElapsed++;
        ticksRemaining--;

        if (!continuousUse) {
            mc.options.keyUse.setDown(true);
            if (ticksRemaining <= 0) {
                stop(mc);
            }
            return;
        }

        mc.options.keyUse.setDown(true);

        if (player.isUsingItem()) {
            sawUsingItem = true;
        } else if (sawUsingItem || ticksElapsed > STARTUP_GRACE_TICKS) {
            stop(mc);
            return;
        }

        if (ticksRemaining <= 0) {
            stop(mc);
        }
    }

    private static boolean isHotbarSlotReady(LocalPlayer player) {
        if (targetHotbarSlot < 0 || targetHotbarSlot >= player.getInventory().items.size()) {
            return false;
        }

        ItemStack stack = player.getInventory().items.get(targetHotbarSlot);
        return !stack.isEmpty();
    }

    private static void stop(Minecraft mc) {
        targetHotbarSlot = -1;
        continuousUse = false;
        syncWaitTicks = 0;
        ticksRemaining = 0;
        ticksElapsed = 0;
        activated = false;
        sawUsingItem = false;
        if (mc != null) {
            mc.options.keyUse.setDown(false);
        }
    }
}
