package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScrollableItemList {

    private static final int ROW_HEIGHT = 40;
    private static final int MAX_VISIBLE = 7;
    private static final int ICON_BOX_SIZE = 24;
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int LINE_GRAY = 0x666A6C6C;
    private static final int ICON_BOX = 0x66303638;
    private static final int ICON_CORNER = 0xAA6A6C6C;
    private static final int SCROLL_TRACK = 0x44000000;
    private static final int SCROLL_THUMB = 0xAA6A6C6C;

    private final Minecraft mc = Minecraft.getInstance();

    private final int x;
    private final int y;
    private final int width;

    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;

    private final List<ItemStack> items;
    private final IScpInventory inventory;
    private final String fixedTypeLabel;

    public ScrollableItemList(int x, int y, int width, List<ItemStack> items, IScpInventory inventory) {
        this(x, y, width, items, inventory, null);
    }

    public ScrollableItemList(int x, int y, int width, List<ItemStack> items, IScpInventory inventory, String fixedTypeLabel) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.items = items;
        this.inventory = inventory;
        this.fixedTypeLabel = fixedTypeLabel;
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

        renderScrollbar(g, slots.size());
    }

    private void renderRow(GuiGraphics g, ItemStack stack, int slotIndex, int rowY) {
        int dropX = x + 4;
        int iconX = x + 28;
        int textX = x + 62;
        int iconY = rowY + 7;

        g.drawString(mc.font, "X", dropX, rowY + 14, TEXT_WHITE, false);

        if (!stack.isEmpty()) {
            drawIconFrame(g, iconX, iconY);
            g.renderItem(stack, iconX + 4, iconY + 4);
        }

        g.drawString(mc.font, stack.getHoverName(), textX, rowY + 8, TEXT_WHITE, false);
        g.drawString(mc.font, getTypeLabel(slotIndex), textX, rowY + 21, TEXT_GRAY, false);

        int lineY = rowY + ROW_HEIGHT - 1;
        g.fill(x + 18, lineY, x + width - 18, lineY + 1, LINE_GRAY);
    }

    private void drawIconFrame(GuiGraphics g, int x, int y) {
        int right = x + ICON_BOX_SIZE;
        int bottom = y + ICON_BOX_SIZE;
        int corner = 6;

        g.fill(x, y, right, bottom, ICON_BOX);

        g.fill(x, y, x + corner, y + 1, ICON_CORNER);
        g.fill(x, y, x + 1, y + corner, ICON_CORNER);
        g.fill(right - corner, y, right, y + 1, ICON_CORNER);
        g.fill(right - 1, y, right, y + corner, ICON_CORNER);
        g.fill(x, bottom - 1, x + corner, bottom, ICON_CORNER);
        g.fill(x, bottom - corner, x + 1, bottom, ICON_CORNER);
        g.fill(right - corner, bottom - 1, right, bottom, ICON_CORNER);
        g.fill(right - 1, bottom - corner, right, bottom, ICON_CORNER);
    }

    private String getTypeLabel(int slotIndex) {
        return fixedTypeLabel != null ? fixedTypeLabel : inventory.getItemType(slotIndex);
    }

    private void renderScrollbar(GuiGraphics g, int totalItems) {
        if (totalItems <= MAX_VISIBLE) {
            return;
        }

        int trackX = getScrollbarX();
        int trackY = y;
        int trackHeight = getScrollbarHeight();
        int thumbHeight = getThumbHeight(totalItems);
        int thumbY = getThumbY(totalItems, thumbHeight);

        g.fill(trackX, trackY, trackX + 5, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + 5, thumbY + thumbHeight, SCROLL_THUMB);
    }

    public boolean mouseScrolled(double delta) {
        List<Integer> slots = getNonEmptySlots();

        if (delta < 0) scrollOffset++;
        if (delta > 0) scrollOffset--;

        clampScroll(slots.size());
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOverScrollbar(mouseX, mouseY)) {
            return false;
        }

        draggingScrollbar = true;
        updateScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseDraggedScrollbar(double mouseY) {
        if (!draggingScrollbar) {
            return false;
        }

        updateScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleasedScrollbar(int button) {
        if (button != 0 || !draggingScrollbar) {
            return false;
        }

        draggingScrollbar = false;
        return true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int totalItems = getNonEmptySlots().size();
        if (totalItems <= MAX_VISIBLE) {
            scrollOffset = 0;
            return;
        }

        int thumbHeight = getThumbHeight(totalItems);
        int trackHeight = getScrollbarHeight();
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int maxScroll = Math.max(1, totalItems - MAX_VISIBLE);
        double relative = (mouseY - y - (thumbHeight / 2.0D)) / thumbTravel;

        scrollOffset = (int) Math.round(relative * maxScroll);
        clampScroll(totalItems);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int totalItems = getNonEmptySlots().size();
        if (totalItems <= MAX_VISIBLE) {
            return false;
        }

        int trackX = getScrollbarX();
        return mouseX >= trackX - 3
                && mouseX <= trackX + 8
                && mouseY >= y
                && mouseY <= y + getScrollbarHeight();
    }

    private void clampScroll(int totalItems) {
        int max = Math.max(0, totalItems - MAX_VISIBLE);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    private int getScrollbarX() {
        return x + width - 10;
    }

    private int getScrollbarHeight() {
        return MAX_VISIBLE * ROW_HEIGHT;
    }

    private int getThumbHeight(int totalItems) {
        return Math.max(18, getScrollbarHeight() * MAX_VISIBLE / totalItems);
    }

    private int getThumbY(int totalItems, int thumbHeight) {
        int maxScroll = Math.max(1, totalItems - MAX_VISIBLE);
        int thumbTravel = getScrollbarHeight() - thumbHeight;
        return y + (thumbTravel * scrollOffset / maxScroll);
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
        return mouseX >= x + 4 && mouseX <= x + 15;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + (MAX_VISIBLE * ROW_HEIGHT);
    }
}
