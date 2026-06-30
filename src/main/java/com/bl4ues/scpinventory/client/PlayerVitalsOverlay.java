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
    private static final String HEALTH_ICON_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAFQ0lEQVR42u1WXYhVVRT+1tr73DP3TuMPZCX0YwhRD1mPEcRVg7RSy5ojEqKORmSOFNRDb2fmLYie/BkLyZ+mqG5iWkOU/cyll15MKAx8EAURS9BxZrw/59yz1+rhnHvnOozOKNhLLtj3Z++11/r2Wt9aewO35f8uNJ1CGIY8DDCGh4HF6Vy5b9iBSK9SVKViX58BhrOJxVgMSH9/v9w0uDAM+XrApvp9DT26oQiEYchN5MGbGx5nNWtUZC4RgZh+j6L48KHdgxeCIDAAUCqV3MrNa+/u6CysZqXHVAUguhwn7quvB/Yfm2zzugCCIDClUskt37bc79L577PiDWOtIQBEBLYGcRSfqdQqK77Z8/kJAFi5ee2iWV2zjnR2dT5ANBGMaqUi9Xq0++yFK+/8VirVmravCaCJ8qXeDQ8b5b2W+ImoVlcicswMMIEJ6vm+51T+ieP4GRGhfD7/QyFfuMuJNAggotQsERm2hsbGxo5Vx0bXH9lb+gthyGiLBE12HvRueB5Cg+rcnCRuJMxsiRjEhDQFDCgk15HjuBGPEhHy+cJscU7YGCbK9KhlOjHW2Gq1NlqLKusO7vzk2/Z0TERAQcW+0Nz59+k/IPJII4obbIxHBFwFIANBgBprKQWkysYQc7Y2CYQCjZyf80YuXTrxxfb7Fqn2KWVVxC2mEvTe2rl7RNyCOIoVRFZVoAqo6sRA+h8AiXMqzikAUhWoKFQUaNuDdNEmjUSNMQtWbTp5DxFps3JsOwcSYR8KBpSgUCD7yo4BABABiKGcpVAVogIWhrICqlAVEDFUNY0CEbJPj63m231eBYAFnqhaFQWYAFUQZb41HewxAIUIwAxAFRBAWAGRLKcMhoCYISIgIqimpEzA3lRVQAD0lXe3zK1fHD8V1+qzMxa3SAUCjOeBTVZmohN8SPsDmBjMKV+4xQUGCGKsRbVyZXQ0Pr9waGBopOmzWbQaBIH57L2BEc6ZQ4WuOxhEjq1JWc9p/ZMhuKgBFQEMQ0XSoQpIGnqRbDhp4w4cW+YkaRwaGhgayRqYTu4DFIYhnbpyKh9F3mFD/HS9Uk3YsAXSCLhGkm4ggs15QEY4yk4NBlrZJsCzHpg58Xzfjo9f/vHSSPLikwsX1vr7+3UqAK1UrHt7XWe9Zr6TOHkqqtYTtmyh1FaKALGByVlo4tI5a0AAGGlashQkuQ7fVquVXy+OnH326ODRStNHi3eTAGgYhjz4wWClc96cVcb3jvuFDqtO0qNrClwVUHFQJyBrUsKKQN1EuYpzifGsrdSqx8cv1l84Oni0kpWezvgyWv3Wa/NNHA+7evxQFEfOsDEEAM1IgGB9r8WBJvEAcrmOnGk4d/JyVF/y/Uefnp/xZTT5Ugp6X30Q0hhu1OL7G43YMRuTlnYz19RiOzNBAef7vkkgZ6LYLTn84f4zU11C10pBS0qlkguCwJR27DltGctsR+689Twj4lza6ZA1Hc16hMI5cdZa41TOxdJYNp3zGb2IisWiLZfLSbB146MQ/SmqRvOcS4SZuUXItOuJ53vMnr3gxC09uOvAiebe69nn6QCUy+WkGIa2tHPfnzB2Ra7gjxljWEUkjQAg4sQYw2ztmMKtPLjrwIliOL3zGUWgFYmwaMv95STo3bhUnQxFlbqvqkpEYGbKFfIRGXruy+0f/9LUnYldnimAcn85KYZFW9qx72drbLdf6HDZYxS5vC+kuuZGnd+UFMOiBYA1Wze93P36eu3esl6DbT3d7Wu3XIphaAGge2tPT/fWnp72uf9MZvosv6USBIFpPs1vy83Kv2ly5IowMMILAAAAAElFTkSuQmCC";

    private static final int ICON_SOURCE_SIZE = 32;
    private static final int ICON_SIZE = 17;
    private static final int BAR_WIDTH = 184;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_X = 60;
    private static final int ICON_X = 36;
    private static final int BOTTOM_MARGIN = 84;
    private static final int BAR_GAP = 18;

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
        g.drawString(mc.font, healthText, BAR_X + 6, healthY + 1, TEXT, false);
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
