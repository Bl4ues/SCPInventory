package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public final class PlayerVitalsOverlay {

    private static final String STAMINA_ICON_BASE64 = "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";
    private static final String HEALTH_ICON_BASE64 = "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";

    private static final int ICON_SOURCE_SIZE = 64;
    private static final int ICON_SIZE = 20;
    private static final int BAR_WIDTH = 320;
    private static final int BAR_HEIGHT = 14;
    private static final int BAR_X = 78;
    private static final int ICON_X = 44;
    private static final int BOTTOM_MARGIN = 84;
    private static final int BAR_GAP = 23;

    private static final int TRACK = 0x7710181B;
    private static final int TRACK_DARK = 0xAA0B1012;
    private static final int BORDER = 0x996A6C6C;
    private static final int TEXT = 0xE8DDE3E0;
    private static final int STAMINA_LEFT = 0xAA4D6474;
    private static final int STAMINA_RIGHT = 0xCC7EA0B7;
    private static final int FLASH_RED = 0xFFE01010;

    private static ResourceLocation staminaIcon;
    private static ResourceLocation healthIcon;
    private static boolean texturesReady = false;

    private PlayerVitalsOverlay() {
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || mc.options.hideGui) return;

        ensureTextures(mc);

        int staminaY = screenHeight - BOTTOM_MARGIN;
        int healthY = staminaY + BAR_GAP;

        drawIcon(g, staminaIcon, ICON_X, staminaY - 4);
        drawIcon(g, healthIcon, ICON_X, healthY - 4);

        drawBar(g, BAR_X, staminaY, BAR_WIDTH, BAR_HEIGHT, PlayerVitalsClient.getStaminaRatio(), STAMINA_LEFT, STAMINA_RIGHT, 0.0F);

        float health = Math.max(0.0F, player.getHealth());
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float healthRatio = Math.max(0.0F, Math.min(1.0F, health / maxHealth));
        int healthColor = getHealthColor(healthRatio);
        int healthDark = darken(healthColor, 0.62F);
        drawBar(g, BAR_X, healthY, BAR_WIDTH, BAR_HEIGHT, healthRatio, healthDark, healthColor, PlayerVitalsClient.getDamageFlashAlpha());

        String healthText = Math.round(health) + "/" + Math.round(maxHealth);
        g.drawString(mc.font, healthText, BAR_X + 8, healthY + 3, TEXT, false);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height, float ratio, int leftColor, int rightColor, float flashAlpha) {
        int right = x + width;
        int bottom = y + height;
        g.fill(x, y, right, bottom, TRACK);
        g.fill(x + 1, y + 1, right - 1, bottom - 1, TRACK_DARK);

        int fillWidth = Math.max(0, Math.min(width - 2, Math.round((width - 2) * ratio)));
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float t = fillWidth <= 1 ? 1.0F : i / (float) (fillWidth - 1);
                g.fill(x + 1 + i, y + 1, x + 2 + i, bottom - 1, lerpColor(leftColor, rightColor, t));
            }

            int markerX = Math.min(right - 2, x + fillWidth);
            g.fill(markerX, y - 2, markerX + 1, bottom + 2, withAlpha(rightColor, 0.9F));

            if (flashAlpha > 0.0F) {
                g.fill(x + 1, y + 1, x + 1 + fillWidth, bottom - 1, withAlpha(FLASH_RED, flashAlpha));
            }
        }

        g.fill(x, y, right, y + 1, BORDER);
        g.fill(x, bottom - 1, right, bottom, BORDER);
        g.fill(x, y, x + 1, bottom, BORDER);
        g.fill(right - 1, y, right, bottom, BORDER);
    }

    private static int getHealthColor(float ratio) {
        int red = 0xCC8C1515;
        int orange = 0xCCAA6C24;
        int green = 0xCC7EA38A;

        if (ratio < 0.25F) return lerpColor(red, orange, ratio / 0.25F);
        if (ratio < 0.60F) return lerpColor(orange, green, (ratio - 0.25F) / 0.35F);
        return lerpColor(green, 0xD09BC0A0, (ratio - 0.60F) / 0.40F);
    }

    private static int darken(int color, float factor) {
        int a = color >>> 24;
        int r = Math.round(((color >> 16) & 0xFF) * factor);
        int g = Math.round(((color >> 8) & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static void drawIcon(GuiGraphics g, ResourceLocation texture, int x, int y) {
        if (texture == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.88F);
        g.blit(texture, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void ensureTextures(Minecraft mc) {
        if (texturesReady) return;
        staminaIcon = registerEmbeddedTexture(mc, "hud/stamina", STAMINA_ICON_BASE64);
        healthIcon = registerEmbeddedTexture(mc, "hud/healthlogo", HEALTH_ICON_BASE64);
        texturesReady = true;
    }

    private static ResourceLocation registerEmbeddedTexture(Minecraft mc, String name, String base64) {
        ResourceLocation location = new ResourceLocation(ScpInventoryMod.MODID, name);
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
                NativeImage image = NativeImage.read(input);
                mc.getTextureManager().register(location, new DynamicTexture(image));
            }
            return location;
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
