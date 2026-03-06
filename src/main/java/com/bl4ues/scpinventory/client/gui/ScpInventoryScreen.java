package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.client.gui.components.ContextMenu;
import com.bl4ues.scpinventory.client.gui.components.ScrollableItemList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

        if (itemList != null)
            itemList.render(g, mouseX, mouseY);

        contextMenu.render(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {

        if (itemList != null)
            return itemList.mouseScrolled(delta);

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (itemList == null) return false;

        int index = itemList.getClickedIndex(mouseX, mouseY);

        if (index == -1) return false;

        if (index < 0 || index >= inventory.getInventory().size()) return false;

        ItemStack stack = inventory.getInventory().get(index);

        if (stack.isEmpty()) return false;

        // Botão esquerdo
        if (button == 0) {

            if (itemList.clickedDrop(mouseX)) {

                inventory.dropItem(
                        index,
                        Minecraft.getInstance().player.level(),
                        Minecraft.getInstance().player.getX(),
                        Minecraft.getInstance().player.getY(),
                        Minecraft.getInstance().player.getZ()
                );

                return true;
            }

        }

        // Botão direito
        if (button == 1) {

            contextIndex = index;

            contextMenu.open(
                    (int) mouseX,
                    (int) mouseY,
                    inventory.getItemType(index)
            );

            return true;
        }

        if (contextMenu.isOpen()) {

            int option = contextMenu.clicked(mouseX, mouseY);

            if (option != -1) {

                String action = contextMenu.getOption(option);

                handleAction(action);

                contextMenu.close();

                return true;
            }

        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleAction(String action) {

        if (contextIndex < 0) return;

        ItemStack stack = inventory.getInventory().get(contextIndex);

        switch (action) {

            case "DROP" -> inventory.dropItem(
                    contextIndex,
                    Minecraft.getInstance().player.level(),
                    Minecraft.getInstance().player.getX(),
                    Minecraft.getInstance().player.getY(),
                    Minecraft.getInstance().player.getZ()
            );

            case "USE" -> {
                Minecraft.getInstance().player.eat(
                        Minecraft.getInstance().player.level(),
                        stack
                );
                inventory.removeInventoryItem(contextIndex);
            }

            case "EQUIP" -> {

                String type = inventory.getItemType(contextIndex);

                if (type.equals("Head")) inventory.setHead(stack);
                if (type.equals("Body")) inventory.setChest(stack);
                if (type.equals("Legs")) inventory.setLegs(stack);
                if (type.equals("Feet")) inventory.setFeet(stack);

                inventory.removeInventoryItem(contextIndex);
            }

        }

    }

}