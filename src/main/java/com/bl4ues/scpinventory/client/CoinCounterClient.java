package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class CoinCounterClient {

    private static final int BACKGROUND_SOURCE_WIDTH = 1406;
    private static final int BACKGROUND_SOURCE_HEIGHT = 1080;
    private static final int INVENTORY_TAB_WIDTH = 90;
    private static final int KEYS_TAB_WIDTH = 76;
    private static final int TAB_HEIGHT = 17;
    private static final int ICON_BOX_SIZE = 24;
    private static final int ICON_BOX = 0x66303638;
    private static final int ICON_BORDER = 0xAA6A6C6C;
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;

    private static Field modeField;

    private CoinCounterClient() {
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ScpInventoryScreen screen) || !isInventoryMode(screen)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ItemStack coinStack = ScpItemClassifier.getConfiguredCoinStack();
        Optional<ResourceLocation> coinId = ScpItemClassifier.getConfiguredCoinItemId();
        if (coinStack.isEmpty() || coinId.isEmpty()) {
            return;
        }

        int count = Math.min(ScpPickupRouter.MAX_COIN_COUNT, countCoins(mc.player.getInventory(), coinId.get()));
        renderCounter(event.getGuiGraphics(), mc, coinStack, count);
    }

    private static void renderCounter(GuiGraphics g, Minecraft mc, ItemStack coinStack, int count) {
        Layout layout = computeLayout(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        String text = Integer.toString(count);
        int textWidth = mc.font.width(text);
        int totalWidth = ICON_BOX_SIZE + 6 + textWidth;
        int minX = layout.keysX() + KEYS_TAB_WIDTH + 8;
        int right = layout.listPanelX() + layout.listPanelWidth() - 10;
        int x = Math.max(minX, right - totalWidth - 12);
        int y = layout.tabY() - ((ICON_BOX_SIZE - TAB_HEIGHT) / 2);

        drawIconFrame(g, x, y);
        g.renderItem(coinStack, x + 4, y + 4);
        g.drawString(mc.font, text, x + ICON_BOX_SIZE + 6, y + 8, TEXT_WHITE, false);
        g.fill(x + ICON_BOX_SIZE + 4, y + ICON_BOX_SIZE - 3, x + ICON_BOX_SIZE + 6 + textWidth, y + ICON_BOX_SIZE - 2, TEXT_GRAY);
    }

    private static int countCoins(Inventory inventory, ResourceLocation coinId) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            count += countMatchingCoins(stack, coinId);
        }
        for (ItemStack stack : inventory.offhand) {
            count += countMatchingCoins(stack, coinId);
        }
        for (ItemStack stack : inventory.armor) {
            count += countMatchingCoins(stack, coinId);
        }
        return count;
    }

    private static int countMatchingCoins(ItemStack stack, ResourceLocation coinId) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        return coinId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) ? stack.getCount() : 0;
    }

    private static boolean isInventoryMode(Screen screen) {
        try {
            if (modeField == null) {
                modeField = ScpInventoryScreen.class.getDeclaredField("mode");
                modeField.setAccessible(true);
            }

            return "INVENTORY".equals(String.valueOf(modeField.get(screen)));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Layout computeLayout(int width, int height) {
        int margin = 24;
        int availableWidth = width - (margin * 2);
        int availableHeight = height - (margin * 2);
        float aspect = BACKGROUND_SOURCE_WIDTH / (float) BACKGROUND_SOURCE_HEIGHT;

        int rootHeight = availableHeight;
        int rootWidth = Math.round(rootHeight * aspect);
        if (rootWidth > availableWidth) {
            rootWidth = availableWidth;
            rootHeight = Math.round(rootWidth / aspect);
        }

        int rootX = (width - rootWidth) / 2;
        int rootY = (height - rootHeight) / 2;
        int titleY = rootY + Math.round(rootHeight * 0.105F);
        int tabY = titleY + Math.round(rootHeight * 0.043F);
        int sideMargin = Math.round(rootWidth * 0.055F);
        int panelGap = Math.round(rootWidth * 0.040F);
        int sharedPanelWidth = (rootWidth - (sideMargin * 2) - panelGap) / 2;
        int listPanelX = rootX + sideMargin;
        int listX = listPanelX + 18;
        int keysX = listX + INVENTORY_TAB_WIDTH + 14;

        return new Layout(listPanelX, sharedPanelWidth, keysX, tabY);
    }

    private static void drawIconFrame(GuiGraphics g, int x, int y) {
        int right = x + ICON_BOX_SIZE;
        int bottom = y + ICON_BOX_SIZE;
        int corner = 6;
        g.fill(x, y, right, bottom, ICON_BOX);
        g.fill(x, y, x + corner, y + 1, ICON_BORDER);
        g.fill(x, y, x + 1, y + corner, ICON_BORDER);
        g.fill(right - corner, y, right, y + 1, ICON_BORDER);
        g.fill(right - 1, y, right, y + corner, ICON_BORDER);
        g.fill(x, bottom - 1, x + corner, bottom, ICON_BORDER);
        g.fill(x, bottom - corner, x + 1, bottom, ICON_BORDER);
        g.fill(right - corner, bottom - 1, right, bottom, ICON_BORDER);
        g.fill(right - 1, bottom - corner, right, bottom, ICON_BORDER);
    }

    private record Layout(int listPanelX, int listPanelWidth, int keysX, int tabY) {
    }
}
