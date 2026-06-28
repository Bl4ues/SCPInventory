package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.client.ClientInventoryBridge;
import com.bl4ues.scpinventory.client.gui.components.ContextMenu;
import com.bl4ues.scpinventory.client.gui.components.ScrollableItemList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ScpInventoryScreen extends Screen {

    private ScrollableItemList itemList;
    private ContextMenu contextMenu;
    private IScpInventory inventory;
    private int contextIndex = -1;

    public ScpInventoryScreen() {
        super(Component.literal("SCP Inventory"));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        contextMenu = new ContextMenu();

        if (mc.player == null) {
            return;
        }

        mc.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv -> {
            inventory = inv;
            itemList = new ScrollableItemList(
                    width / 2 - 100,
                    height / 2 - 120,
                    200,
                    inv.getInventory(),
                    inv
            );
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        if (itemList != null) {
            itemList.render(g, mouseX, mouseY);
        }

        if (contextMenu != null) {
            contextMenu.render(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (itemList != null) {
            return itemList.mouseScrolled(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contextMenu != null && contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option != -1) {
                handleAction(contextMenu.getOption(option));
                contextMenu.close();
                return true;
            }
            contextMenu.close();
        }

        if (itemList == null || inventory == null) {
            return false;
        }

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
            contextMenu.open((int) mouseX, (int) mouseY, inventory.getItemType(index));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleAction(String action) {
        if (contextIndex >= 0) {
            ClientInventoryBridge.perform(contextIndex, action);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
