package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CodexPanel {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int LINE_GRAY = 0x556A6C6C;
    private static final int TAB_ACTIVE = 0x449FC8C8;
    private static final int ICON_BOX = 0x553F4648;
    private static final int SCROLL_TRACK = 0x33000000;
    private static final int SCROLL_THUMB = 0x886A6C6C;

    private static final int ROW_HEIGHT = 28;
    private static final int MAX_VISIBLE_DOCUMENTS = 12;

    private final Minecraft mc = Minecraft.getInstance();
    private final IScpInventory inventory;
    private final int x;
    private final int y;
    private final int listWidth;
    private final int detailX;
    private final int detailWidth;
    private final int titleY;
    private final int listTitleX;
    private final int detailTitleX;

    private int selectedIndex = -1;
    private int scrollOffset = 0;

    public CodexPanel(int x, int y, int listWidth, int detailX, int detailWidth, int titleY, int listTitleX, int detailTitleX, IScpInventory inventory) {
        this.x = x;
        this.y = y;
        this.listWidth = listWidth;
        this.detailX = detailX;
        this.detailWidth = detailWidth;
        this.titleY = titleY;
        this.listTitleX = listTitleX;
        this.detailTitleX = detailTitleX;
        this.inventory = inventory;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitle(g, listTitleX, titleY, "CLASSIFICATION");
        drawSectionTitle(g, detailTitleX, titleY, "DOCUMENT");
        renderDocumentList(g);
        renderDocumentDetails(g);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        int index = getClickedDocumentIndex(mouseX, mouseY);
        if (index >= 0 && index < inventory.getDocuments().size()) {
            selectedIndex = index;
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOverList(mouseX, mouseY)) {
            return false;
        }

        int total = inventory.getDocuments().size();
        if (delta < 0) scrollOffset++;
        if (delta > 0) scrollOffset--;

        int max = Math.max(0, total - MAX_VISIBLE_DOCUMENTS);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;

        return true;
    }

    private void renderDocumentList(GuiGraphics g) {
        List<ItemStack> documents = inventory.getDocuments();

        if (selectedIndex >= documents.size()) {
            selectedIndex = documents.isEmpty() ? -1 : documents.size() - 1;
        }

        for (int i = 0; i < MAX_VISIBLE_DOCUMENTS; i++) {
            int docIndex = scrollOffset + i;
            if (docIndex >= documents.size()) {
                break;
            }

            int rowY = y + (i * ROW_HEIGHT);
            ItemStack document = documents.get(docIndex);
            String name = ScpItemClassifier.getCodexDisplayName(document);

            if (docIndex == selectedIndex) {
                g.fill(x, rowY - 2, x + listWidth - 14, rowY + ROW_HEIGHT - 2, TAB_ACTIVE);
            }

            g.drawString(mc.font, name, x + 10, rowY + 7, TEXT_WHITE, false);

            int lineY = rowY + ROW_HEIGHT - 1;
            g.fill(x, lineY, x + listWidth - 20, lineY + 1, LINE_GRAY);
        }

        renderScrollbar(g, documents.size());
    }

    private void renderDocumentDetails(GuiGraphics g) {
        List<ItemStack> documents = inventory.getDocuments();
        if (selectedIndex < 0 || selectedIndex >= documents.size()) {
            return;
        }

        ItemStack document = documents.get(selectedIndex);
        String title = ScpItemClassifier.getCodexDisplayName(document);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(document.getItem());

        int iconX = detailX;
        int iconY = y;
        g.fill(iconX, iconY, iconX + 26, iconY + 26, ICON_BOX);
        g.renderItem(document, iconX + 5, iconY + 5);

        g.drawString(mc.font, title, detailX + 36, y + 2, TEXT_WHITE, false);
        g.drawString(mc.font, "Document", detailX + 36, y + 15, TEXT_GRAY, false);

        int lineY = y + 36;
        g.fill(detailX, lineY, detailX + detailWidth, lineY + 1, LINE_GRAY);

        List<String> detailLines = new ArrayList<>();
        detailLines.add("Item: " + itemId);
        if (document.hasTag() && document.getTag() != null) {
            for (String key : document.getTag().getAllKeys()) {
                detailLines.add(key + ": " + document.getTag().get(key));
            }
        } else {
            detailLines.add("No NBT data stored on this document.");
        }

        int textY = y + 48;
        for (String line : detailLines) {
            textY = drawWrapped(g, line, detailX, textY, detailWidth, TEXT_GRAY) + 6;
            if (textY > y + 330) {
                drawWrapped(g, "...", detailX, textY, detailWidth, TEXT_GRAY);
                break;
            }
        }
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = mc.font.split(Component.literal(text), width);
        int cursorY = y;
        for (FormattedCharSequence line : lines) {
            g.drawString(mc.font, line, x, cursorY, color, false);
            cursorY += 11;
        }
        return cursorY;
    }

    private int getClickedDocumentIndex(double mouseX, double mouseY) {
        if (!isMouseOverList(mouseX, mouseY)) {
            return -1;
        }

        int row = (int) ((mouseY - y) / ROW_HEIGHT);
        if (row < 0 || row >= MAX_VISIBLE_DOCUMENTS) {
            return -1;
        }

        return scrollOffset + row;
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= x
                && mouseX <= x + listWidth
                && mouseY >= y
                && mouseY <= y + (MAX_VISIBLE_DOCUMENTS * ROW_HEIGHT);
    }

    private void renderScrollbar(GuiGraphics g, int totalItems) {
        if (totalItems <= MAX_VISIBLE_DOCUMENTS) {
            return;
        }

        int trackX = x + listWidth - 10;
        int trackY = y;
        int trackHeight = MAX_VISIBLE_DOCUMENTS * ROW_HEIGHT;
        int thumbHeight = Math.max(18, trackHeight * MAX_VISIBLE_DOCUMENTS / totalItems);
        int maxScroll = Math.max(1, totalItems - MAX_VISIBLE_DOCUMENTS);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + (thumbTravel * scrollOffset / maxScroll);

        g.fill(trackX, trackY, trackX + 5, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + 5, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://CODEX_";
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false);
    }
}
