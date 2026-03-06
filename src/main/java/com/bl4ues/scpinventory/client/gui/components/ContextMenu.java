package com.bl4ues.scpinventory.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class ContextMenu {

    private final Minecraft mc = Minecraft.getInstance();

    private int x;
    private int y;

    private boolean open = false;

    private final List<String> options = new ArrayList<>();

    public void open(int x, int y, String type) {

        this.x = x;
        this.y = y;

        options.clear();

        options.add("DROP");

        if (type.equals("Consumable")) {
            options.add("USE");
        }

        if (type.equals("Head") || type.equals("Body") || type.equals("Legs") || type.equals("Feet")) {
            options.add("EQUIP");
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

        int height = options.size() * 14 + 4;

        g.fill(x, y, x + 60, y + height, 0xFF222222);

        for (int i = 0; i < options.size(); i++) {

            g.drawString(
                    mc.font,
                    options.get(i),
                    x + 4,
                    y + 4 + (i * 14),
                    0xFFFFFF,
                    false
            );
        }
    }

    public int clicked(double mouseX, double mouseY) {

        if (!open) return -1;

        if (mouseX < x || mouseX > x + 60) return -1;

        int index = (int) ((mouseY - y) / 14);

        if (index < 0 || index >= options.size()) return -1;

        return index;
    }

    public String getOption(int index) {
        return options.get(index);
    }
}