package com.bl4ues.scpinventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class InventoryFullOverlay {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("scpinventory", "textures/gui/inventoryfull.png");

    private static boolean active = false;
    private static long startTime = 0L;

    private static final long TOTAL_DURATION = 5000L; // duração total em ms
    private static final long FADE_TIME = 500L;       // fade in/out em ms

    // Posição/padding na tela
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 10;

    // Tamanho da imagem na tela (ajuste para deixar pequena)
    private static final int DISPLAY_WIDTH = 225;
    private static final int DISPLAY_HEIGHT = 52;

    // Tamanho real da textura original
    private static final int TEXTURE_WIDTH = 1623;
    private static final int TEXTURE_HEIGHT = 376;

    /**
     * Mostra o overlay (uma vez por tentativa de pickup)
     */
    public static void show() {
        if (active) return;
        active = true;
        startTime = System.currentTimeMillis();
    }

    /**
     * Renderiza o overlay na tela
     */
    public static void render(GuiGraphics guiGraphics) {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= TOTAL_DURATION) {
            active = false;
            return;
        }

        // Calcula alpha para fade in/out
        float alpha;
        if (elapsed < FADE_TIME) {
            alpha = (float) elapsed / FADE_TIME;
        } else if (elapsed > TOTAL_DURATION - FADE_TIME) {
            alpha = (float) (TOTAL_DURATION - elapsed) / FADE_TIME;
        } else {
            alpha = 1.0f;
        }

        // Posição na tela
        int x = PADDING_X;
        int y = PADDING_Y;

        // Configura blend e shader
        RenderSystem.enableBlend();                  // ativa blending
        RenderSystem.defaultBlendFunc();            // função de blend padrão (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha); // alpha = 1f para opaco ou <1f para fade
        guiGraphics.blit(
                TEXTURE,
                x, y,
                0f, 0f,
                DISPLAY_WIDTH, DISPLAY_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );
        RenderSystem.disableBlend();                // desativa blending
    }
}