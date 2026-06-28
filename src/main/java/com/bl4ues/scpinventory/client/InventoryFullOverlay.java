package com.bl4ues.scpinventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class InventoryFullOverlay {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("scpinventory", "textures/gui/inventoryfull.png");

    private static final long VISIBLE_DURATION = 2500L;
    private static final long FADE_IN_TIME = 150L;
    private static final long FADE_OUT_TIME = 500L;

    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 10;
    private static final int DISPLAY_WIDTH = 225;
    private static final int DISPLAY_HEIGHT = 52;
    private static final int TEXTURE_WIDTH = 1623;
    private static final int TEXTURE_HEIGHT = 376;

    private static boolean active = false;
    private static long shownAt = 0L;
    private static long visibleUntil = 0L;

    public static void show() {
        long now = System.currentTimeMillis();
        if (!active) {
            active = true;
            shownAt = now;
        }
        visibleUntil = Math.max(visibleUntil, now + VISIBLE_DURATION);
    }

    public static void render(GuiGraphics guiGraphics) {
        if (!active) {
            return;
        }

        long now = System.currentTimeMillis();
        long visibleFor = now - shownAt;
        long remaining = visibleUntil - now;

        if (remaining <= 0L) {
            active = false;
            return;
        }

        float alpha = getAlpha(visibleFor, remaining);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000);
        guiGraphics.blit(
                TEXTURE,
                PADDING_X,
                PADDING_Y,
                0f,
                0f,
                DISPLAY_WIDTH,
                DISPLAY_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
        guiGraphics.pose().popPose();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static float getAlpha(long visibleFor, long remaining) {
        float fadeInAlpha = FADE_IN_TIME <= 0L
                ? 1.0f
                : Math.min(1.0f, (float) visibleFor / FADE_IN_TIME);

        float fadeOutAlpha = remaining < FADE_OUT_TIME
                ? Math.max(0f, (float) remaining / FADE_OUT_TIME)
                : 1.0f;

        return Math.min(fadeInAlpha, fadeOutAlpha);
    }
}
