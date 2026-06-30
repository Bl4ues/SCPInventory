package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class KeyCountTextOverlay {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventory_background.png");
    private static final int BACKGROUND_SOURCE_WIDTH = 1406;
    private static final int BACKGROUND_SOURCE_HEIGHT = 1080;
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int ROOT_TINT = 0x11000000;
    private static final String OLD_SUFFIX = " key(s) in inventory";
    private static final String NEW_SUFFIX = " of 12 keys";

    private KeyCountTextOverlay() {
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ScpInventoryScreen screen)) {
            return;
        }

        try {
            if (!getBoolean(screen, "showingKeys")) {
                return;
            }
            if (getFloat(screen, "dropPreviewFade") > 0.01F) {
                return;
            }

            IScpInventory inventory = (IScpInventory) get(screen, "inventory");
            if (inventory == null) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.font == null) {
                return;
            }

            int listX = getInt(screen, "listX");
            int listWidth = getInt(screen, "listWidth");
            int titleY = getInt(screen, "titleY");
            int rootX = getInt(screen, "rootX");
            int rootY = getInt(screen, "rootY");
            int rootWidth = getInt(screen, "rootWidth");
            int rootHeight = getInt(screen, "rootHeight");

            String countText = Integer.toString(inventory.getKeyCount());
            int oldWidth = mc.font.width(countText) + mc.font.width(OLD_SUFFIX);
            int newWidth = mc.font.width(countText) + mc.font.width(NEW_SUFFIX);
            int right = listX + listWidth;
            int eraseX = right - Math.max(oldWidth, newWidth) - 4;
            int eraseY = titleY - 2;
            int eraseWidth = Math.max(oldWidth, newWidth) + 8;
            int eraseHeight = mc.font.lineHeight + 5;

            GuiGraphics g = event.getGuiGraphics();
            redrawBackgroundSlice(g, eraseX, eraseY, eraseWidth, eraseHeight, rootX, rootY, rootWidth, rootHeight);
            g.fill(eraseX, eraseY, eraseX + eraseWidth, eraseY + eraseHeight, ROOT_TINT);

            int x = right - newWidth;
            g.drawString(mc.font, countText, x, titleY, TEXT_WHITE, false);
            g.drawString(mc.font, NEW_SUFFIX, x + mc.font.width(countText), titleY, TEXT_GRAY, false);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Keep the base screen safe if the layout internals change later.
        }
    }

    private static void redrawBackgroundSlice(GuiGraphics g, int x, int y, int width, int height, int rootX, int rootY, int rootWidth, int rootHeight) {
        int clippedX = Math.max(x, rootX);
        int clippedY = Math.max(y, rootY);
        int clippedRight = Math.min(x + width, rootX + rootWidth);
        int clippedBottom = Math.min(y + height, rootY + rootHeight);
        if (clippedRight <= clippedX || clippedBottom <= clippedY) {
            return;
        }

        float u = (clippedX - rootX) * (BACKGROUND_SOURCE_WIDTH / (float) rootWidth);
        float v = (clippedY - rootY) * (BACKGROUND_SOURCE_HEIGHT / (float) rootHeight);
        int sourceWidth = Math.max(1, Math.round((clippedRight - clippedX) * (BACKGROUND_SOURCE_WIDTH / (float) rootWidth)));
        int sourceHeight = Math.max(1, Math.round((clippedBottom - clippedY) * (BACKGROUND_SOURCE_HEIGHT / (float) rootHeight)));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(BACKGROUND, clippedX, clippedY, clippedRight - clippedX, clippedBottom - clippedY, u, v, sourceWidth, sourceHeight, BACKGROUND_SOURCE_WIDTH, BACKGROUND_SOURCE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static Object get(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int getInt(Object target, String fieldName) throws ReflectiveOperationException {
        return ((Number) get(target, fieldName)).intValue();
    }

    private static float getFloat(Object target, String fieldName) throws ReflectiveOperationException {
        return ((Number) get(target, fieldName)).floatValue();
    }

    private static boolean getBoolean(Object target, String fieldName) throws ReflectiveOperationException {
        return (Boolean) get(target, fieldName);
    }
}
