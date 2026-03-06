package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScrollableItemList {

    private final Minecraft mc = Minecraft.getInstance();

    private final int x;
    private final int y;
    private final int width;

    private final int ROW_HEIGHT = 28;
    private final int MAX_VISIBLE = 7;

    private int scrollOffset = 0;

    private final List<ItemStack> items;
    private final IScpInventory inventory;

    public ScrollableItemList(int x, int y, int width, List<ItemStack> items, IScpInventory inventory) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.items = items;
        this.inventory = inventory;
    }

    private List<Integer> getNonEmptySlots() {

        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                slots.add(i);
            }
        }

        return slots;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {

        List<Integer> slots = getNonEmptySlots();

        for (int i = 0; i < MAX_VISIBLE; i++) {

            int listIndex = scrollOffset + i;

            if (listIndex >= slots.size()) break;

            int slotIndex = slots.get(listIndex);

            ItemStack stack = items.get(slotIndex);

            int rowY = y + (i * ROW_HEIGHT);

            renderRow(g, stack, slotIndex, rowY);
        }
    }

    private void renderRow(GuiGraphics g, ItemStack stack, int slotIndex, int rowY) {

        int dropX = x + 4;
        int iconX = x + 22;
        int textX = x + 42;

        g.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0x66000000);

        g.drawString(
                mc.font,
                "X",
                dropX,
                rowY + 9,
                0xFF5555,
                false
        );

        g.renderItem(stack, iconX, rowY + 6);

        g.drawString(
                mc.font,
                stack.getHoverName(),
                textX,
                rowY + 4,
                0xFFFFFF,
                false
        );

        g.drawString(
                mc.font,
                inventory.getItemType(slotIndex),
                textX,
                rowY + 16,
                0xAAAAAA,
                false
        );
    }

    public boolean mouseScrolled(double delta) {

        List<Integer> slots = getNonEmptySlots();

        if (delta < 0) scrollOffset++;
        if (delta > 0) scrollOffset--;

        int max = Math.max(0, slots.size() - MAX_VISIBLE);

        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;

        return true;
    }

    public int getClickedIndex(double mouseX, double mouseY) {

        int row = (int) ((mouseY - y) / ROW_HEIGHT);

        if (row < 0 || row >= MAX_VISIBLE) return -1;

        List<Integer> slots = getNonEmptySlots();

        int index = scrollOffset + row;

        if (index >= slots.size()) return -1;

        return slots.get(index);
    }

    public boolean clickedDrop(double mouseX) {
        return mouseX >= x + 4 && mouseX <= x + 14;
    }
}