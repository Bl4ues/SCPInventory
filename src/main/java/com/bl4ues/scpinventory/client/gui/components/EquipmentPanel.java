package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class EquipmentPanel {

    private static final int ROW_HEIGHT = 42;
    private static final int ICON_BOX_SIZE = 22;
    private static final int TEXT_WHITE = 0xFFEAEAEA;
    private static final int TEXT_GRAY = 0xFF9F9F9F;
    private static final int LINE_GRAY = 0x559A9A9A;
    private static final int ICON_BOX = 0x553F4648;

    private final Minecraft mc = Minecraft.getInstance();
    private final int x;
    private final int y;
    private final int width;
    private final IScpInventory inventory;

    public EquipmentPanel(int x, int y, int width, IScpInventory inventory) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.inventory = inventory;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitle(g, x, y - 18, "EQUIPMENT");

        int rowY = y;
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
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
        ScpEquipmentSlot[] slots = ScpEquipmentSlot.values();
        if (row < 0 || row >= slots.length) {
            return null;
        }

        return slots[row];
    }

    private void renderSlot(GuiGraphics g, ScpEquipmentSlot slot, int rowY) {
        ItemStack stack = inventory.getEquipment(slot);

        int iconX = x + 8;
        int iconY = rowY + 8;
        int textX = x + 40;

        if (!stack.isEmpty()) {
            g.fill(iconX, iconY, iconX + ICON_BOX_SIZE, iconY + ICON_BOX_SIZE, ICON_BOX);
            g.renderItem(stack, iconX + 3, iconY + 3);
        }

        Component itemName = stack.isEmpty() ? Component.literal("None") : stack.getHoverName();

        g.drawString(mc.font, slot.getDisplayName(), textX, rowY + 8, TEXT_WHITE, false);
        g.drawString(
                mc.font,
                itemName,
                textX,
                rowY + 21,
                stack.isEmpty() ? TEXT_GRAY : TEXT_WHITE,
                false
        );

        int lineY = rowY + ROW_HEIGHT - 1;
        g.fill(x, lineY, x + width, lineY + 1, LINE_GRAY);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://INVENTORY_";
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false);
    }
}
