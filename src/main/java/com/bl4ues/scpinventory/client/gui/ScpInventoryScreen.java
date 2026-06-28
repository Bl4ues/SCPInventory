package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.client.ClientInventoryBridge;
import com.bl4ues.scpinventory.client.gui.components.CodexPanel;
import com.bl4ues.scpinventory.client.gui.components.ContextMenu;
import com.bl4ues.scpinventory.client.gui.components.EquipmentPanel;
import com.bl4ues.scpinventory.client.gui.components.ScrollableItemList;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.EquipmentActionPacket;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.KeyActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ScpInventoryScreen extends Screen {
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TAB_ACTIVE = 0x55B2B3B3;
    private static final int TAB_INACTIVE = 0x336A6C6C;
    private static final int ROOT_TINT = 0x11000000;
    private static final int PANEL_BACKGROUND = 0x88545D5F;
    private static final int FOOTER_BACKGROUND = 0x2A2B3133;

    private static final ResourceLocation BACKGROUND = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventory_background.png");
    private static final ResourceLocation INVENTORY_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventoryicon.png");
    private static final ResourceLocation INVENTORY_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventoryicon_selected.png");
    private static final ResourceLocation CODEX_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/codexicon.png");
    private static final ResourceLocation CODEX_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/codexicon_selected.png");
    private static final ResourceLocation HEALTH_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/health.png");

    private static final int BACKGROUND_SOURCE_WIDTH = 1406;
    private static final int BACKGROUND_SOURCE_HEIGHT = 1080;
    private static final int SOURCE_ICON_SIZE = 128;
    private static final int NAV_ICON_SIZE = 24;
    private static final int NAV_BUTTON_WIDTH = 120;
    private static final int NAV_BUTTON_HEIGHT = 46;
    private static final int TAB_HEIGHT = 20;
    private static final int INVENTORY_TAB_WIDTH = 90;
    private static final int KEYS_TAB_WIDTH = 76;
    private static final int HEALTH_ICON_SIZE = 24;

    private enum ScreenMode { INVENTORY, CODEX }

    private ScrollableItemList itemList;
    private EquipmentPanel equipmentPanel;
    private CodexPanel codexPanel;
    private ContextMenu contextMenu;
    private IScpInventory inventory;
    private int contextIndex = -1;
    private boolean contextIsKey;
    private boolean showingKeys;
    private ScreenMode mode = ScreenMode.INVENTORY;

    private int rootX, rootY, rootWidth, rootHeight;
    private int titleY, tabY, navY;
    private int listPanelX, listPanelY, listPanelWidth, listPanelHeight;
    private int equipmentPanelX, equipmentPanelY, equipmentPanelWidth, equipmentPanelHeight;
    private int listX, listY, listWidth;
    private int equipmentX, equipmentY, equipmentWidth;

    public ScpInventoryScreen() {
        super(Component.literal("SCP Inventory"));
    }

    @Override
    protected void init() {
        contextMenu = new ContextMenu();
        computeLayout();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv -> {
            inventory = inv;
            rebuildItemList();
            equipmentPanel = new EquipmentPanel(equipmentX, equipmentY, equipmentWidth, titleY, inv);
            codexPanel = new CodexPanel(listX, listPanelY + 26, listWidth, equipmentX, equipmentWidth, titleY, inv);
        });
    }

    private void computeLayout() {
        int margin = 24;
        int availableWidth = width - (margin * 2);
        int availableHeight = height - (margin * 2);
        float aspect = BACKGROUND_SOURCE_WIDTH / (float) BACKGROUND_SOURCE_HEIGHT;

        rootHeight = availableHeight;
        rootWidth = Math.round(rootHeight * aspect);
        if (rootWidth > availableWidth) {
            rootWidth = availableWidth;
            rootHeight = Math.round(rootWidth / aspect);
        }

        rootX = (width - rootWidth) / 2;
        rootY = (height - rootHeight) / 2;

        titleY = rootY + Math.round(rootHeight * 0.105F);
        tabY = titleY + Math.round(rootHeight * 0.043F);
        navY = rootY + rootHeight - Math.round(rootHeight * 0.105F);

        listPanelX = rootX + Math.round(rootWidth * 0.038F);
        listPanelY = tabY - 5;
        listPanelWidth = Math.round(rootWidth * 0.455F);
        listPanelHeight = Math.round(rootHeight * 0.590F);

        listX = listPanelX + 18;
        listY = tabY + 34;
        listWidth = listPanelWidth - 36;

        equipmentPanelX = rootX + Math.round(rootWidth * 0.530F);
        equipmentPanelY = titleY + Math.round(rootHeight * 0.045F);
        equipmentPanelWidth = rootX + rootWidth - equipmentPanelX - Math.round(rootWidth * 0.038F);
        equipmentPanelHeight = Math.round(rootHeight * 0.595F);

        equipmentX = equipmentPanelX + 28;
        equipmentY = equipmentPanelY + 56;
        equipmentWidth = equipmentPanelWidth - 56;
    }

    private void rebuildItemList() {
        if (inventory == null) return;
        itemList = showingKeys
                ? new ScrollableItemList(listX, listY, listWidth, inventory.getKeys(), inventory, "Key")
                : new ScrollableItemList(listX, listY, listWidth, inventory.getInventory(), inventory);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        renderPanels(g);
        renderHealthStatus(g);

        if (mode == ScreenMode.CODEX) {
            if (codexPanel != null) codexPanel.render(g, mouseX, mouseY);
        } else {
            renderInventoryHeader(g);
            renderTabs(g);
            if (itemList != null) itemList.render(g, mouseX, mouseY);
            if (equipmentPanel != null) equipmentPanel.render(g, mouseX, mouseY);
        }

        renderBottomNavigation(g);
        if (contextMenu != null) contextMenu.render(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderPanels(GuiGraphics g) {
        g.blit(BACKGROUND, rootX, rootY, rootWidth, rootHeight, 0.0F, 0.0F, BACKGROUND_SOURCE_WIDTH, BACKGROUND_SOURCE_HEIGHT, BACKGROUND_SOURCE_WIDTH, BACKGROUND_SOURCE_HEIGHT);
        g.fill(rootX, rootY, rootX + rootWidth, rootY + rootHeight, ROOT_TINT);
        g.fill(rootX, navY - 18, rootX + rootWidth, rootY + rootHeight, FOOTER_BACKGROUND);

        if (mode == ScreenMode.CODEX) {
            g.fill(listPanelX, titleY + 24, listPanelX + listPanelWidth, rootY + rootHeight - 112, PANEL_BACKGROUND);
            g.fill(equipmentPanelX, titleY + 24, equipmentPanelX + equipmentPanelWidth, rootY + rootHeight - 112, PANEL_BACKGROUND);
        } else {
            g.fill(listPanelX, listPanelY, listPanelX + listPanelWidth, listPanelY + listPanelHeight, PANEL_BACKGROUND);
            g.fill(equipmentPanelX, equipmentPanelY, equipmentPanelX + equipmentPanelWidth, equipmentPanelY + equipmentPanelHeight, PANEL_BACKGROUND);
        }
    }

    private void renderHealthStatus(GuiGraphics g) {
        if (minecraft == null || minecraft.player == null) return;
        int healthX = rootX + Math.round(rootWidth * 0.040F);
        int healthY = rootY + Math.round(rootHeight * 0.040F);
        int textX = healthX + HEALTH_ICON_SIZE + 8;
        blitFullIcon(g, HEALTH_ICON, healthX, healthY + 3, HEALTH_ICON_SIZE, HEALTH_ICON_SIZE);

        int health = Math.round(minecraft.player.getHealth());
        int maxHealth = Math.round(minecraft.player.getMaxHealth());
        int percent = maxHealth <= 0 ? 0 : Math.round((health / (float) maxHealth) * 100.0F);
        g.drawString(minecraft.font, "VIDA", textX, healthY, TEXT_WHITE, false);
        g.drawString(minecraft.font, percent + "/100", textX, healthY + 13, TEXT_WHITE, false);
    }

    private void renderInventoryHeader(GuiGraphics g) {
        drawSectionTitle(g, listX, titleY, "BACKPACK");
        String count = inventory == null
                ? "0 of 12 items"
                : showingKeys
                ? inventory.getKeyCount() + " of " + IScpInventory.MAX_KEY_COUNT + " keys"
                : inventory.getInventoryCount() + " of " + inventory.getMaxMainSlots() + " items";
        g.drawString(minecraft.font, count, listX + listWidth - minecraft.font.width(count), titleY, TEXT_WHITE, false);
    }

    private void renderTabs(GuiGraphics g) {
        drawTab(g, listX, tabY, INVENTORY_TAB_WIDTH, "INVENTÁRIO", !showingKeys);
        drawTab(g, listX + INVENTORY_TAB_WIDTH + 14, tabY, KEYS_TAB_WIDTH, "KEYS", showingKeys);
    }

    private void renderBottomNavigation(GuiGraphics g) {
        drawNavigationButton(g, getInventoryNavX(), navY, "INVENTÁRIO", mode == ScreenMode.INVENTORY ? INVENTORY_ICON_SELECTED : INVENTORY_ICON, mode == ScreenMode.INVENTORY);
        drawNavigationButton(g, getCodexNavX(), navY, "CODEX", mode == ScreenMode.CODEX ? CODEX_ICON_SELECTED : CODEX_ICON, mode == ScreenMode.CODEX);
    }

    private void drawNavigationButton(GuiGraphics g, int x, int y, String label, ResourceLocation icon, boolean active) {
        int iconX = x + (NAV_BUTTON_WIDTH - NAV_ICON_SIZE) / 2;
        int textX = x + (NAV_BUTTON_WIDTH - minecraft.font.width(label)) / 2;
        blitFullIcon(g, icon, iconX, y, NAV_ICON_SIZE, NAV_ICON_SIZE);
        g.drawString(minecraft.font, label, textX, y + NAV_ICON_SIZE + 6, active ? TEXT_WHITE : TEXT_GRAY, false);
    }

    private void blitFullIcon(GuiGraphics g, ResourceLocation icon, int x, int y, int width, int height) {
        g.blit(icon, x, y, width, height, 0.0F, 0.0F, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE);
    }

    private void drawTab(GuiGraphics g, int x, int y, int w, String label, boolean active) {
        g.fill(x, y, x + w, y + TAB_HEIGHT, active ? TAB_ACTIVE : TAB_INACTIVE);
        g.drawString(minecraft.font, label, x + (w - minecraft.font.width(label)) / 2, y + 6, active ? TEXT_WHITE : TEXT_GRAY, false);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://INVENTORY_";
        g.drawString(minecraft.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(minecraft.font, suffix, x + minecraft.font.width(prefix), y, TEXT_WHITE, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mode == ScreenMode.CODEX && codexPanel != null && codexPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.isMouseOver(mouseX, mouseY)) return itemList.mouseScrolled(delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.mouseClickedScrollbar(mouseX, mouseY, button)) return true;
        if (button == 0 && clickedBottomNavigation(mouseX, mouseY)) return true;
        if (mode == ScreenMode.CODEX) return codexPanel != null && codexPanel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && clickedTabs(mouseX, mouseY)) return true;

        if (contextMenu != null && contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option != -1) {
                handleAction(contextMenu.getOption(option));
                contextMenu.close();
                return true;
            }
            contextMenu.close();
        }

        if (inventory == null) return false;

        if (equipmentPanel != null) {
            ScpEquipmentSlot clickedEquipmentSlot = equipmentPanel.getClickedSlot(mouseX, mouseY);
            if (clickedEquipmentSlot != null && !inventory.getEquipment(clickedEquipmentSlot).isEmpty()) {
                if (button == 0) {
                    ClientInventoryBridge.performEquipment(clickedEquipmentSlot, EquipmentActionPacket.ACTION_UNEQUIP);
                    return true;
                }
                if (button == 1) {
                    ClientInventoryBridge.performEquipment(clickedEquipmentSlot, EquipmentActionPacket.ACTION_DROP);
                    return true;
                }
            }
        }

        if (itemList == null) return false;
        return showingKeys ? handleKeyClick(mouseX, mouseY, button) : handleMainInventoryClick(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.mouseDraggedScrollbar(mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.mouseReleasedScrollbar(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean handleMainInventoryClick(double mouseX, double mouseY, int button) {
        int index = itemList.getClickedIndex(mouseX, mouseY);
        if (!inventory.isValidMainSlot(index)) return false;
        ItemStack stack = inventory.getInventoryItem(index);
        if (stack.isEmpty()) return false;

        if (button == 0 && itemList.clickedDrop(mouseX)) {
            ClientInventoryBridge.perform(index, InventoryActionPacket.ACTION_DROP);
            return true;
        }
        if (button == 1) {
            contextIndex = index;
            contextIsKey = false;
            contextMenu.open((int) mouseX, (int) mouseY, ScpItemClassifier.getEquipmentSlot(stack).isPresent() ? "Head" : inventory.getItemType(index));
            return true;
        }
        return false;
    }

    private boolean handleKeyClick(double mouseX, double mouseY, int button) {
        int index = itemList.getClickedIndex(mouseX, mouseY);
        if (index < 0 || index >= inventory.getKeys().size()) return false;
        ItemStack key = inventory.getKeys().get(index);
        if (key.isEmpty()) return false;

        if (button == 0 && itemList.clickedDrop(mouseX)) {
            ClientInventoryBridge.performKey(index, KeyActionPacket.ACTION_DROP);
            return true;
        }
        if (button == 1) {
            contextIndex = index;
            contextIsKey = true;
            contextMenu.open((int) mouseX, (int) mouseY, "Key");
            return true;
        }
        return false;
    }

    private boolean clickedTabs(double mouseX, double mouseY) {
        if (mouseY < tabY || mouseY > tabY + TAB_HEIGHT) return false;
        if (mouseX >= listX && mouseX <= listX + INVENTORY_TAB_WIDTH) {
            showingKeys = false;
            contextIsKey = false;
            rebuildItemList();
            return true;
        }
        int keysX = listX + INVENTORY_TAB_WIDTH + 14;
        if (mouseX >= keysX && mouseX <= keysX + KEYS_TAB_WIDTH) {
            showingKeys = true;
            contextIsKey = false;
            rebuildItemList();
            return true;
        }
        return false;
    }

    private boolean clickedBottomNavigation(double mouseX, double mouseY) {
        if (mouseY < navY || mouseY > navY + NAV_BUTTON_HEIGHT) return false;
        int inventoryX = getInventoryNavX();
        int codexX = getCodexNavX();
        if (mouseX >= inventoryX && mouseX <= inventoryX + NAV_BUTTON_WIDTH) {
            mode = ScreenMode.INVENTORY;
            return true;
        }
        if (mouseX >= codexX && mouseX <= codexX + NAV_BUTTON_WIDTH) {
            mode = ScreenMode.CODEX;
            if (contextMenu != null) contextMenu.close();
            return true;
        }
        return false;
    }

    private int getInventoryNavX() {
        return rootX + (rootWidth / 2) - 225;
    }

    private int getCodexNavX() {
        return rootX + (rootWidth / 2) + 105;
    }

    private void handleAction(String action) {
        if (contextIndex < 0) return;
        if (contextIsKey) ClientInventoryBridge.performKey(contextIndex, action);
        else ClientInventoryBridge.perform(contextIndex, action);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
