package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class UsableItemHoldClient {

    private static final int MAX_HOLD_TICKS = 120;
    private static final int STARTUP_GRACE_TICKS = 24;
    private static final int STARTUP_DELAY_TICKS = 6;

    private static int ticksRemaining = 0;
    private static int ticksElapsed = 0;
    private static int delayTicks = 0;
    private static boolean sawUsingItem = false;
    private static boolean clickOnly = false;

    private UsableItemHoldClient() {
    }

    public static void start(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            stop(Minecraft.getInstance());
            return;
        }

        clickOnly = stack.getUseAnimation() == UseAnim.NONE;
        ticksRemaining = clickOnly ? 2 : MAX_HOLD_TICKS;
        ticksElapsed = 0;
        delayTicks = STARTUP_DELAY_TICKS;
        sawUsingItem = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ticksRemaining <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stop(mc);
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        ticksElapsed++;
        ticksRemaining--;
        mc.options.keyUse.setDown(true);

        if (clickOnly) {
            if (ticksElapsed >= 2 || ticksRemaining <= 0) {
                stop(mc);
            }
            return;
        }

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

    private static void stop(Minecraft mc) {
        ticksRemaining = 0;
        ticksElapsed = 0;
        delayTicks = 0;
        sawUsingItem = false;
        clickOnly = false;
        if (mc != null) {
            mc.options.keyUse.setDown(false);
        }
    }
}
