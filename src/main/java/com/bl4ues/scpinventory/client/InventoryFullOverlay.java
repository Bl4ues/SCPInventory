package com.bl4ues.scpinventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class InventoryFullOverlay {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("scpinventory", "textures/gui/inventoryfull.png");

    private static final long TOTAL_DURATION = 5000L;
    private static final long FADE_TIME = 500L;

    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 10;
    private static final int DISPLAY_WIDTH = 225;
    private static final int DISPLAY_HEIGHT = 52;
    private static final int TEXTURE_WIDTH = 1623;
    private static final int TEXTURE_HEIGHT = 376;

    private static boolean active = false;
    private static long startTime = 0L;

    public static void show() {
        active = true;
        startTime = System.currentTimeMillis();
    }

    public static void render(GuiGraphics guiGraphics) {
        if (!active) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= TOTAL_DURATION) {
            active = false;
            return;
        }

        float alpha = getAlpha(elapsed);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
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
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static float getAlpha(long elapsed) {
        if (elapsed < FADE_TIME) {
            return (float) elapsed / FADE_TIME;
        }

        if (elapsed > TOTAL_DURATION - FADE_TIME) {
            return (float) (TOTAL_DURATION - elapsed) / FADE_TIME;
        }

        return 1.0f;
    }
}
