package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class EquipmentPanel {

    private static final ScpEquipmentSlot[] DISPLAY_SLOTS = {
            ScpEquipmentSlot.HEAD,
            ScpEquipmentSlot.CHEST,
            ScpEquipmentSlot.LEGS,
            ScpEquipmentSlot.FEET,
            ScpEquipmentSlot.ACCESSORY,
            ScpEquipmentSlot.WEAPON
    };

    private static final int ROW_HEIGHT = 37;
    private static final int ICON_BOX_SIZE = 24;
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int LINE_GRAY = 0x666A6C6C;
    private static final int ICON_BOX = 0x66303638;
    private static final int ICON_BORDER = 0xAA6A6C6C;

    private final Minecraft mc = Minecraft.getInstance();
    private final int x;
    private final int y;
    private final int width;
    private final int titleY;
    private final int titleX;
    private final IScpInventory inventory;

    public EquipmentPanel(int x, int y, int width, int titleY, int titleX, IScpInventory inventory) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.titleY = titleY;
        this.titleX = titleX;
        this.inventory = inventory;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitle(g, titleX, titleY, "EQUIPMENT");

        int rowY = y;
        for (ScpEquipmentSlot slot : DISPLAY_SLOTS) {
            renderSlot(g, slot, rowY);
            rowY += ROW_HEIGHT;
        }
    }

    public ScpEquipmentSlot getClickedSlot(double mouseX, double mouseY) {
        if (mouseX < x || mouseX > x + width) {
            return null;
        }

        int relativeY = (int) (mouseY - y);
        if (relativeY < 0) {
            return null;
        }

        int row = relativeY / ROW_HEIGHT;
        if (row < 0 || row >= DISPLAY_SLOTS.length) {
            return null;
        }

        return DISPLAY_SLOTS[row];
    }

    public ItemStack getDisplayedStack(ScpEquipmentSlot slot) {
        if (slot == null || inventory == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = inventory.getEquipment(slot);
        if (!stack.isEmpty()) {
            return stack;
        }

        if (slot == ScpEquipmentSlot.ACCESSORY && mc.player != null && ScpItemClassifier.isAccessoryHand(mc.player.getOffhandItem())) {
            ItemStack offhand = mc.player.getOffhandItem().copy();
            offhand.setCount(1);
            inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, offhand.copy());
            return offhand;
        }

        return ItemStack.EMPTY;
    }

    private void renderSlot(GuiGraphics g, ScpEquipmentSlot slot, int rowY) {
        ItemStack stack = getDisplayedStack(slot);

        int iconX = x + 8;
        int iconY = rowY + 6;
        int textX = x + 44;

        if (stack.isEmpty()) {
            drawEmptyIconCorners(g, iconX, iconY);
        } else {
            drawFilledIconFrame(g, iconX, iconY);
            g.renderItem(stack, iconX + 4, iconY + 4);
        }

        Component itemName = stack.isEmpty() ? Component.literal("None") : stack.getHoverName();

        g.drawString(mc.font, slot.getDisplayName(), textX, rowY + 7, TEXT_WHITE, false);
        g.drawString(
                mc.font,
                itemName,
                textX,
                rowY + 20,
                stack.isEmpty() ? TEXT_GRAY : TEXT_WHITE,
                false
        );

        int lineY = rowY + ROW_HEIGHT - 1;
        g.fill(x, lineY, x + width, lineY + 1, LINE_GRAY);
    }

    private void drawFilledIconFrame(GuiGraphics g, int x, int y) {
        int right = x + ICON_BOX_SIZE;
        int bottom = y + ICON_BOX_SIZE;

        g.fill(x, y, right, bottom, ICON_BOX);
        g.fill(x, y, right, y + 1, ICON_BORDER);
        g.fill(x, bottom - 1, right, bottom, ICON_BORDER);
        g.fill(x, y, x + 1, bottom, ICON_BORDER);
        g.fill(right - 1, y, right, bottom, ICON_BORDER);
    }

    private void drawEmptyIconCorners(GuiGraphics g, int x, int y) {
        int right = x + ICON_BOX_SIZE;
        int bottom = y + ICON_BOX_SIZE;
        int corner = 6;

        g.fill(x, y, x + corner, y + 1, ICON_BORDER);
        g.fill(x, y, x + 1, y + corner, ICON_BORDER);
        g.fill(right - corner, y, right, y + 1, ICON_BORDER);
        g.fill(right - 1, y, right, y + corner, ICON_BORDER);
        g.fill(x, bottom - 1, x + corner, bottom, ICON_BORDER);
        g.fill(x, bottom - corner, x + 1, bottom, ICON_BORDER);
        g.fill(right - corner, bottom - 1, right, bottom, ICON_BORDER);
        g.fill(right - 1, bottom - corner, right, bottom, ICON_BORDER);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://INVENTORY_";
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false);
    }
}
