package com.bl4ues.scpinventory.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class ContextMenu {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_SELECTED = 0xFF202020;
    private static final int MENU_BACKGROUND = 0xBB303638;
    private static final int OPTION_HOVER = 0x55B2B3B3;
    private static final int LINE_GRAY = 0x666A6C6C;
    private static final int HINT_BACKGROUND = 0x33303638;
    private static final int MENU_WIDTH = 118;
    private static final int OPTION_HEIGHT = 22;
    private static final int OPTION_TOP = 4;
    private static final int OPTION_GAP = 6;
    private static final int HINT_TOP_GAP = 7;
    private static final int HINT_INSET_LEFT = 4;
    private static final int HINT_INSET_RIGHT = 1;
    private static final int HINT_TEXT_PADDING_X = 2;
    private static final int HINT_LINE_HEIGHT = 9;
    private static final int HINT_PADDING_TOP = 3;
    private static final int HINT_PADDING_BOTTOM = -3;
    private static final float HINT_TEXT_SCALE = 0.82F;
    private static final int MENU_Z = 500;

    private final Minecraft mc = Minecraft.getInstance();

    private int x;
    private int y;
    private boolean open = false;
    private HintMode hintMode = HintMode.NONE;

    private final List<MenuOption> options = new ArrayList<>();

    public void open(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.hintMode = HintMode.NONE;

        options.clear();

        if ("Consumable".equals(type)) {
            options.add(new MenuOption("USE", "Use Item"));
            options.add(new MenuOption("DROP", "Drop Item"));
            hintMode = HintMode.CONSUME;
        } else if (isEquipmentType(type)) {
            options.add(new MenuOption("EQUIP", "Equip Item"));
            options.add(new MenuOption("DROP", "Drop Item"));
            hintMode = HintMode.EQUIP;
        } else {
            options.add(new MenuOption("DROP", "Drop Item"));
        }

        open = true;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (!open) return;

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, MENU_Z);

        int height = getMenuHeight();
        g.fill(x, y, x + MENU_WIDTH, y + height, MENU_BACKGROUND);

        for (int i = 0; i < options.size(); i++) {
            int optionY = getOptionY(i);
            boolean hovered = isInside(mouseX, mouseY, x, optionY, MENU_WIDTH, OPTION_HEIGHT);
            if (hovered) {
                g.fill(x, optionY, x + MENU_WIDTH, optionY + OPTION_HEIGHT, OPTION_HOVER);
            }

            String label = options.get(i).label;
            g.drawString(
                    mc.font,
                    label,
                    x + (MENU_WIDTH - mc.font.width(label)) / 2,
                    optionY + 7,
                    hovered ? TEXT_SELECTED : TEXT_WHITE,
                    false
            );

            if (i < options.size() - 1) {
                int lineY = optionY + OPTION_HEIGHT + 1;
                g.fill(x + 16, lineY, x + MENU_WIDTH - 16, lineY + 1, LINE_GRAY);
            }
        }

        if (hintMode != HintMode.NONE) {
            int hintY = getOptionY(options.size() - 1) + OPTION_HEIGHT + HINT_TOP_GAP;
            int hintLeft = x + HINT_INSET_LEFT;
            int hintRight = x + MENU_WIDTH - HINT_INSET_RIGHT;
            int textX = hintLeft + HINT_TEXT_PADDING_X;
            int maxTextWidth = getHintTextWidth();
            List<String> lines = wrapHintText(getHintText(), maxTextWidth);
            int hintHeight = HINT_PADDING_TOP + (lines.size() * HINT_LINE_HEIGHT) + HINT_PADDING_BOTTOM;

            g.fill(hintLeft, hintY - HINT_PADDING_TOP, hintRight, hintY + hintHeight - HINT_PADDING_TOP, HINT_BACKGROUND);

            int lineY = hintY;
            for (String line : lines) {
                drawScaledString(g, line, textX, lineY, TEXT_WHITE, HINT_TEXT_SCALE);
                lineY += HINT_LINE_HEIGHT;
            }
        }

        g.pose().popPose();
    }

    public int clicked(double mouseX, double mouseY) {
        if (!open) return -1;
        if (mouseX < x || mouseX > x + MENU_WIDTH) return -1;

        for (int i = 0; i < options.size(); i++) {
            if (isInside(mouseX, mouseY, x, getOptionY(i), MENU_WIDTH, OPTION_HEIGHT)) {
                return i;
            }
        }

        return -1;
    }

    public String getOption(int index) {
        return options.get(index).action;
    }

    private int getOptionY(int index) {
        return y + OPTION_TOP + (index * (OPTION_HEIGHT + OPTION_GAP));
    }

    private int getMenuHeight() {
        int base = OPTION_TOP + options.size() * OPTION_HEIGHT + Math.max(0, options.size() - 1) * OPTION_GAP + OPTION_TOP;
        if (hintMode == HintMode.NONE) {
            return base;
        }

        int lineCount = wrapHintText(getHintText(), getHintTextWidth()).size();
        int hintHeight = HINT_PADDING_TOP + (lineCount * HINT_LINE_HEIGHT) + HINT_PADDING_BOTTOM;
        return base + HINT_TOP_GAP + hintHeight;
    }

    private int getHintTextWidth() {
        int rawWidth = MENU_WIDTH - HINT_INSET_LEFT - HINT_INSET_RIGHT - (HINT_TEXT_PADDING_X * 2);
        return Math.max(1, Math.round(rawWidth / HINT_TEXT_SCALE));
    }

    private String getHintText() {
        return hintMode == HintMode.EQUIP
                ? "You can double click or press Shift + Left Click to EQUIP this item"
                : "You can double click to CONSUME this item";
    }

    private List<String> wrapHintText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            String candidate = current.isEmpty() ? word : current + " " + word;
            if (mc.font.width(candidate) <= maxWidth || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines.isEmpty() ? List.of(text) : lines;
    }

    private boolean isEquipmentType(String type) {
        return type.equals("Head")
                || type.equals("Chest")
                || type.equals("Legs")
                || type.equals("Feet")
                || type.equals("Accessory")
                || type.equals("Body")
                || type.equals("Weapon");
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void drawScaledString(GuiGraphics g, String text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private enum HintMode {
        NONE,
        CONSUME,
        EQUIP
    }

    private static class MenuOption {
        private final String action;
        private final String label;

        private MenuOption(String action, String label) {
            this.action = action;
            this.label = label;
        }
    }
}
