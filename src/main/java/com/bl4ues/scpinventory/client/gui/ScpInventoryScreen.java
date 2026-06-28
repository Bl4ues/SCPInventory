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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ScpInventoryScreen extends Screen {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TAB_ACTIVE = 0x44B2B3B3;
    private static final int TAB_INACTIVE = 0x226A6C6C;

    private static final ResourceLocation INVENTORY_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventoryicon.png");
    private static final ResourceLocation INVENTORY_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventoryicon_selected.png");
    private static final ResourceLocation CODEX_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/codexicon.png");
    private static final ResourceLocation CODEX_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/codexicon_selected.png");
    private static final ResourceLocation HEALTH_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/health.png");

    private static final int NAV_ICON_SIZE = 24;
    private static final int NAV_BUTTON_WIDTH = 120;
    private static final int NAV_BUTTON_HEIGHT = 42;
    private static final int HEALTH_ICON_SIZE = 24;
    private static final int SOURCE_ICON_SIZE = 128;

    private enum ScreenMode {
        INVENTORY,
        CODEX
    }

    private ScrollableItemList itemList;
    private EquipmentPanel equipmentPanel;
    private CodexPanel codexPanel;
    private ContextMenu contextMenu;
    private IScpInventory inventory;
    private int contextIndex = -1;
    private boolean contextIsKey = false;

    private int listX;
    private int listY;
    private int listWidth;
    private int equipmentX;
    private int equipmentY;
    private int navY;
    private boolean showingKeys = false;
    private ScreenMode mode = ScreenMode.INVENTORY;

    public ScpInventoryScreen() {
        super(Component.literal("SCP Inventory"));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        contextMenu = new ContextMenu();

        listX = width / 2 - 330;
        listY = height / 2 - 100;
        listWidth = 320;
        equipmentX = width / 2 + 20;
        equipmentY = listY + 24;
        navY = height - 76;

        int titleY = listY - 48;

        if (mc.player == null) {
            return;
        }

        mc.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv -> {
            inventory = inv;
            rebuildItemList();
            equipmentPanel = new EquipmentPanel(equipmentX, equipmentY, 300, titleY, inv);
            codexPanel = new CodexPanel(listX, listY - 6, 320, equipmentX, 360, titleY, inv);
        });
    }

    private void rebuildItemList() {
        if (inventory == null) {
            return;
        }

        if (showingKeys) {
            itemList = new ScrollableItemList(listX, listY, listWidth, inventory.getKeys(), inventory, "Key");
        } else {
            itemList = new ScrollableItemList(listX, listY, listWidth, inventory.getInventory(), inventory);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        renderHealthStatus(g);

        if (mode == ScreenMode.CODEX) {
            if (codexPanel != null) {
                codexPanel.render(g, mouseX, mouseY);
            }
        } else {
            renderInventoryHeader(g);
            renderTabs(g);

            if (itemList != null) {
                itemList.render(g, mouseX, mouseY);
            }

            if (equipmentPanel != null) {
                equipmentPanel.render(g, mouseX, mouseY);
            }
        }

        renderBottomNavigation(g);

        if (contextMenu != null) {
            contextMenu.render(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHealthStatus(GuiGraphics g) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        int healthX = Math.max(18, listX - 170);
        int healthY = Math.max(18, listY - 92);
        int textX = healthX + HEALTH_ICON_SIZE + 8;

        blitFullIcon(g, HEALTH_ICON, healthX, healthY + 3, HEALTH_ICON_SIZE, HEALTH_ICON_SIZE);

        int health = Math.round(minecraft.player.getHealth());
        int maxHealth = Math.round(minecraft.player.getMaxHealth());

        g.drawString(minecraft.font, "VIDA", textX, healthY, TEXT_WHITE, false);
        g.drawString(minecraft.font, health + "/" + maxHealth, textX, healthY + 13, TEXT_WHITE, false);
    }

    private void renderInventoryHeader(GuiGraphics g) {
        drawSectionTitle(g, listX, listY - 48, "BACKPACK");

        String count = inventory == null
                ? "0 of 12 items"
                : showingKeys
                ? inventory.getKeyCount() + " of " + IScpInventory.MAX_KEY_COUNT + " keys"
                : inventory.getInventoryCount() + " of " + inventory.getMaxMainSlots() + " items";
        g.drawString(minecraft.font, count, listX + listWidth - minecraft.font.width(count), listY - 48, TEXT_WHITE, false);
    }

    private void renderTabs(GuiGraphics g) {
        drawTab(g, listX, listY - 26, 76, "INVENTORY", !showingKeys);
        drawTab(g, listX + 88, listY - 26, 76, "KEYS", showingKeys);
    }

    private void renderBottomNavigation(GuiGraphics g) {
        int inventoryX = width / 2 - 270;
        int codexX = width / 2 + 190;
        drawNavigationButton(
                g,
                inventoryX,
                navY,
                "INVENTORY",
                mode == ScreenMode.INVENTORY ? INVENTORY_ICON_SELECTED : INVENTORY_ICON,
                mode == ScreenMode.INVENTORY
        );
        drawNavigationButton(
                g,
                codexX,
                navY,
                "CODEX",
                mode == ScreenMode.CODEX ? CODEX_ICON_SELECTED : CODEX_ICON,
                mode == ScreenMode.CODEX
        );
    }

    private void drawNavigationButton(GuiGraphics g, int x, int y, String label, ResourceLocation icon, boolean active) {
        int iconX = x + (NAV_BUTTON_WIDTH - NAV_ICON_SIZE) / 2;
        int textX = x + (NAV_BUTTON_WIDTH - minecraft.font.width(label)) / 2;
        int color = active ? TEXT_WHITE : TEXT_GRAY;

        blitFullIcon(g, icon, iconX, y, NAV_ICON_SIZE, NAV_ICON_SIZE);
        g.drawString(minecraft.font, label, textX, y + NAV_ICON_SIZE + 6, color, false);
    }

    private void blitFullIcon(GuiGraphics g, ResourceLocation icon, int x, int y, int width, int height) {
        g.blit(icon, x, y, width, height, 0.0F, 0.0F, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE);
    }

    private void drawTab(GuiGraphics g, int x, int y, int w, String label, boolean active) {
        g.fill(x, y, x + w, y + 20, active ? TAB_ACTIVE : TAB_INACTIVE);
        int textX = x + (w - minecraft.font.width(label)) / 2;
        g.drawString(minecraft.font, label, textX, y + 6, active ? TEXT_WHITE : TEXT_GRAY, false);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://INVENTORY_";
        g.drawString(minecraft.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(minecraft.font, suffix, x + minecraft.font.width(prefix), y, TEXT_WHITE, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mode == ScreenMode.CODEX && codexPanel != null) {
            if (codexPanel.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }

        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.isMouseOver(mouseX, mouseY)) {
            return itemList.mouseScrolled(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickedBottomNavigation(mouseX, mouseY)) {
            return true;
        }

        if (mode == ScreenMode.CODEX) {
            if (codexPanel != null && codexPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && clickedTabs(mouseX, mouseY)) {
            return true;
        }

        if (contextMenu != null && contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option != -1) {
                handleAction(contextMenu.getOption(option));
                contextMenu.close();
                return true;
            }
            contextMenu.close();
        }

        if (inventory == null) {
            return false;
        }

        if (equipmentPanel != null) {
            ScpEquipmentSlot clickedEquipmentSlot = equipmentPanel.getClickedSlot(mouseX, mouseY);
            if (clickedEquipmentSlot != null && !inventory.getEquipment(clickedEquipmentSlot).isEmpty()) {
                if (button == 0) {
                    ClientInventoryBridge.performEquipment(clickedEquipmentSlot, "UNEQUIP");
                    return true;
                }

                if (button == 1) {
                    ClientInventoryBridge.performEquipment(clickedEquipmentSlot, "DROP");
                    return true;
                }
            }
        }

        if (itemList == null) {
            return false;
        }

        if (showingKeys) {
            return handleKeyClick(mouseX, mouseY, button);
        }

        return handleMainInventoryClick(mouseX, mouseY, button);
    }

    private boolean handleMainInventoryClick(double mouseX, double mouseY, int button) {
        int index = itemList.getClickedIndex(mouseX, mouseY);
        if (!inventory.isValidMainSlot(index)) {
            return false;
        }

        ItemStack stack = inventory.getInventoryItem(index);
        if (stack.isEmpty()) {
            return false;
        }

        if (button == 0 && itemList.clickedDrop(mouseX)) {
            ClientInventoryBridge.perform(index, "DROP");
            return true;
        }

        if (button == 1) {
            contextIndex = index;
            contextIsKey = false;
            contextMenu.open((int) mouseX, (int) mouseY, inventory.getItemType(index));
            return true;
        }

        return false;
    }

    private boolean handleKeyClick(double mouseX, double mouseY, int button) {
        int index = itemList.getClickedIndex(mouseX, mouseY);
        if (index < 0 || index >= inventory.getKeys().size()) {
            return false;
        }

        ItemStack key = inventory.getKeys().get(index);
        if (key.isEmpty()) {
            return false;
        }

        if (button == 0 && itemList.clickedDrop(mouseX)) {
            ClientInventoryBridge.performKey(index, "DROP");
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
        int tabY = listY - 26;
        if (mouseY < tabY || mouseY > tabY + 20) {
            return false;
        }

        if (mouseX >= listX && mouseX <= listX + 76) {
            showingKeys = false;
            contextIsKey = false;
            rebuildItemList();
            return true;
        }

        if (mouseX >= listX + 88 && mouseX <= listX + 164) {
            showingKeys = true;
            contextIsKey = false;
            rebuildItemList();
            return true;
        }

        return false;
    }

    private boolean clickedBottomNavigation(double mouseX, double mouseY) {
        if (mouseY < navY || mouseY > navY + NAV_BUTTON_HEIGHT) {
            return false;
        }

        int inventoryX = width / 2 - 270;
        int codexX = width / 2 + 190;

        if (mouseX >= inventoryX && mouseX <= inventoryX + NAV_BUTTON_WIDTH) {
            mode = ScreenMode.INVENTORY;
            return true;
        }

        if (mouseX >= codexX && mouseX <= codexX + NAV_BUTTON_WIDTH) {
            mode = ScreenMode.CODEX;
            if (contextMenu != null) {
                contextMenu.close();
            }
            return true;
        }

        return false;
    }

    private void handleAction(String action) {
        if (contextIndex < 0) {
            return;
        }

        if (contextIsKey) {
            ClientInventoryBridge.performKey(contextIndex, action);
        } else {
            ClientInventoryBridge.perform(contextIndex, action);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
