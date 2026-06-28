package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class EquipmentPanel {

    private static final int ROW_HEIGHT = 34;
    private static final int ICON_SIZE = 18;

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
        g.drawString(mc.font, "//INVENTORY_EQUIPMENT", x, y - 14, 0xFFCCCCCC, false);
        g.fill(x, y, x + width, y + 160, 0x66333333);

        int rowY = y + 12;
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            renderSlot(g, slot, rowY);
            rowY += ROW_HEIGHT;
        }
    }

    public ScpEquipmentSlot getClickedSlot(double mouseX, double mouseY) {
        if (mouseX < x || mouseX > x + width) {
            return null;
        }

        int relativeY = (int) (mouseY - (y + 12));
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

        int iconX = x + 12;
        int textX = x + 40;

        g.fill(x + 8, rowY - 4, x + width - 8, rowY + ROW_HEIGHT - 6, 0x33000000);
        g.fill(iconX, rowY, iconX + ICON_SIZE, rowY + ICON_SIZE, 0x55333333);

        if (!stack.isEmpty()) {
            g.renderItem(stack, iconX + 1, rowY + 1);
        }

        Component itemName = stack.isEmpty() ? Component.literal("None") : stack.getHoverName();

        g.drawString(mc.font, slot.getDisplayName(), textX, rowY, 0xFFDDDDDD, false);
        g.drawString(
                mc.font,
                itemName,
                textX,
                rowY + 12,
                stack.isEmpty() ? 0xFF888888 : 0xFFFFFFFF,
                false
        );
    }
}
