package com.bl4ues.scpinventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class InventoryFullOverlay {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("scpinventory", "textures/gui/inventoryfull.png");

    private static final long VISIBLE_DURATION = 2500L;
    private static final long FADE_OUT_TIME = 500L;

    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 10;
    private static final int DISPLAY_WIDTH = 225;
    private static final int DISPLAY_HEIGHT = 52;
    private static final int TEXTURE_WIDTH = 1623;
    private static final int TEXTURE_HEIGHT = 376;

    private static boolean active = false;
    private static long visibleUntil = 0L;

    public static void show() {
        active = true;
        visibleUntil = System.currentTimeMillis() + VISIBLE_DURATION;
    }

    public static void render(GuiGraphics guiGraphics) {
        if (!active) {
            return;
        }

        long remaining = visibleUntil - System.currentTimeMillis();
        if (remaining <= 0L) {
            active = false;
            return;
        }

        float alpha = getAlpha(remaining);

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

    private static float getAlpha(long remaining) {
        if (remaining < FADE_OUT_TIME) {
            return Math.max(0f, (float) remaining / FADE_OUT_TIME);
        }

        return 1.0f;
    }
}
