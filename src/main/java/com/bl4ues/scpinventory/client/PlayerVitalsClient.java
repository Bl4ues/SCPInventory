package com.bl4ues.scpinventory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class PlayerVitalsClient {

    public static final float MAX_STAMINA = 100.0F;
    private static final float STAMINA_DRAIN_PER_TICK = MAX_STAMINA / (5.0F * 20.0F);
    private static final float STAMINA_REGEN_PER_TICK = MAX_STAMINA / (5.0F * 20.0F);
    private static final int REGEN_DELAY_TICKS = 20;
    private static final long DAMAGE_FLASH_DURATION_MS = 1000L;

    private static float stamina = MAX_STAMINA;
    private static int regenDelayTicks = 0;
    private static float lastHealth = -1.0F;
    private static long damageFlashStartedAt = -1L;

    private PlayerVitalsClient() {
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            stamina = MAX_STAMINA;
            regenDelayTicks = 0;
            lastHealth = -1.0F;
            damageFlashStartedAt = -1L;
            return;
        }

        updateDamageFlash(player);
        updateStamina(mc, player);
    }

    private static void updateDamageFlash(LocalPlayer player) {
        float health = player.getHealth();
        if (lastHealth >= 0.0F && health < lastHealth - 0.01F) {
            damageFlashStartedAt = System.currentTimeMillis();
        }
        lastHealth = health;
    }

    private static void updateStamina(Minecraft mc, LocalPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            stamina = MAX_STAMINA;
            regenDelayTicks = 0;
            return;
        }

        boolean sprintKeyHeld = mc.options.keySprint.isDown();
        boolean spendingStamina = sprintKeyHeld || player.isSprinting();

        if (spendingStamina && stamina > 0.0F) {
            stamina = Math.max(0.0F, stamina - STAMINA_DRAIN_PER_TICK);
            regenDelayTicks = REGEN_DELAY_TICKS;
        } else {
            if (regenDelayTicks > 0) {
                regenDelayTicks--;
            } else if (!sprintKeyHeld && stamina < MAX_STAMINA) {
                stamina = Math.min(MAX_STAMINA, stamina + STAMINA_REGEN_PER_TICK);
            }
        }

        if (stamina <= 0.0F) {
            stamina = 0.0F;
            player.setSprinting(false);
            if (sprintKeyHeld) {
                mc.options.keySprint.setDown(false);
            }
        }
    }

    public static float getStamina() {
        return stamina;
    }

    public static float getMaxStamina() {
        return MAX_STAMINA;
    }

    public static float getStaminaRatio() {
        return MAX_STAMINA <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, stamina / MAX_STAMINA));
    }

    public static float getDamageFlashAlpha() {
        if (damageFlashStartedAt < 0L) return 0.0F;
        long elapsed = System.currentTimeMillis() - damageFlashStartedAt;
        if (elapsed >= DAMAGE_FLASH_DURATION_MS) {
            damageFlashStartedAt = -1L;
            return 0.0F;
        }

        float progress = elapsed / (float) DAMAGE_FLASH_DURATION_MS;
        float fade = progress < 0.18F
                ? progress / 0.18F
                : 1.0F - ((progress - 0.18F) / 0.82F);
        return Math.max(0.0F, Math.min(0.65F, fade * 0.65F));
    }
}
