package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CodexPanel {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int LINE_GRAY = 0x556A6C6C;
    private static final int CATEGORY_BACKGROUND = 0x336A6C6C;
    private static final int SELECTED_BACKGROUND = 0x889FC8C8;
    private static final int BUTTON_BACKGROUND = 0x446A6C6C;
    private static final int BUTTON_BACKGROUND_HOVERED = 0x667A7C7C;
    private static final int ICON_BOX = 0x553F4648;
    private static final int SCROLL_TRACK = 0x33000000;
    private static final int SCROLL_THUMB = 0x886A6C6C;

    private static final int ROW_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 18;
    private static final int SCROLL_WIDTH = 5;

    private final Minecraft mc = Minecraft.getInstance();
    private final int x;
    private final int y;
    private final int listWidth;
    private final int listHeight;
    private final int detailX;
    private final int detailY;
    private final int detailWidth;
    private final int detailHeight;
    private final int titleY;
    private final int listTitleX;
    private final int detailTitleX;

    private final List<ItemStack> documents;
    private final Set<String> collapsedCategories = new HashSet<>();

    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean showingText = false;
    private boolean expandedImage = false;

    public CodexPanel(int x, int y, int listWidth, int listHeight, int detailX, int detailWidth, int detailHeight, int titleY, int listTitleX, int detailTitleX, List<ItemStack> documents) {
        this.x = x;
        this.y = y;
        this.listWidth = listWidth;
        this.listHeight = listHeight;
        this.detailX = detailX;
        this.detailY = y;
        this.detailWidth = detailWidth;
        this.detailHeight = detailHeight;
        this.titleY = titleY;
        this.listTitleX = listTitleX;
        this.detailTitleX = detailTitleX;
        this.documents = documents;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitle(g, listTitleX, titleY, "CLASSIFICATION");
        drawSectionTitle(g, detailTitleX, titleY, "DOCUMENT");
        renderDocumentList(g);
        renderDocumentDetails(g, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        if (isValidSelectedDocument()) {
            if (expandedImage && clickedReturnButton(mouseX, mouseY)) {
                expandedImage = false;
                return true;
            }

            if (!expandedImage && clickedTextButton(mouseX, mouseY)) {
                showingText = !showingText;
                return true;
            }

            if (!expandedImage && clickedExpandButton(mouseX, mouseY)) {
                expandedImage = true;
                showingText = false;
                return true;
            }
        }

        DisplayRow clickedRow = getClickedRow(mouseX, mouseY);
        if (clickedRow == null) {
            return false;
        }

        if (clickedRow.categoryRow) {
            if (collapsedCategories.contains(clickedRow.category)) {
                collapsedCategories.remove(clickedRow.category);
            } else {
                collapsedCategories.add(clickedRow.category);
            }
            clampScroll(buildRows().size());
            return true;
        }

        selectedIndex = clickedRow.documentIndex;
        showingText = false;
        expandedImage = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOverList(mouseX, mouseY)) {
            return false;
        }

        if (delta < 0) scrollOffset++;
        if (delta > 0) scrollOffset--;

        clampScroll(buildRows().size());
        return true;
    }

    private void renderDocumentList(GuiGraphics g) {
        normalizeSelection();

        List<DisplayRow> rows = buildRows();
        clampScroll(rows.size());

        int visibleRows = getVisibleRows();
        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scrollOffset + i;
            if (rowIndex >= rows.size()) {
                break;
            }

            DisplayRow row = rows.get(rowIndex);
            int rowY = y + (i * ROW_HEIGHT);

            if (row.categoryRow) {
                renderCategoryRow(g, row, rowY);
            } else {
                renderDocumentRow(g, row, rowY);
            }
        }

        renderScrollbar(g, rows.size());
    }

    private void renderCategoryRow(GuiGraphics g, DisplayRow row, int rowY) {
        g.fill(x, rowY, x + listWidth - 18, rowY + ROW_HEIGHT, CATEGORY_BACKGROUND);
        g.drawString(mc.font, row.category, x + 10, rowY + 8, TEXT_WHITE, false);
        g.drawString(mc.font, collapsedCategories.contains(row.category) ? ">" : "v", x + listWidth - 38, rowY + 8, TEXT_GRAY, false);
    }

    private void renderDocumentRow(GuiGraphics g, DisplayRow row, int rowY) {
        if (row.documentIndex == selectedIndex) {
            g.fill(x + 6, rowY, x + listWidth - 24, rowY + ROW_HEIGHT, SELECTED_BACKGROUND);
        }

        g.drawString(mc.font, row.name, x + 24, rowY + 8, TEXT_WHITE, false);
    }

    private void renderDocumentDetails(GuiGraphics g, int mouseX, int mouseY) {
        if (!isValidSelectedDocument()) {
            return;
        }

        ItemStack document = documents.get(selectedIndex);
        CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(document);

        if (expandedImage) {
            renderExpandedImage(g, mouseX, mouseY, document, definition);
            return;
        }

        if (showingText) {
            renderDocumentText(g, mouseX, mouseY, document, definition);
            return;
        }

        renderDocumentImagePreview(g, mouseX, mouseY, document, definition);
    }

    private void renderDocumentImagePreview(GuiGraphics g, int mouseX, int mouseY, ItemStack document, CodexDocumentDefinition definition) {
        int buttonY = getButtonY();
        int imageAreaX = detailX + 40;
        int imageAreaY = detailY + 4;
        int imageAreaWidth = detailWidth - 80;
        int imageAreaHeight = Math.max(40, buttonY - imageAreaY - 8);

        drawDocumentImage(g, document, definition, imageAreaX, imageAreaY, imageAreaWidth, imageAreaHeight);
        drawDetailButtons(g, mouseX, mouseY);
    }

    private void renderExpandedImage(GuiGraphics g, int mouseX, int mouseY, ItemStack document, CodexDocumentDefinition definition) {
        drawButton(g, detailX, detailY, 64, BUTTON_HEIGHT, "Return", isMouseInside(mouseX, mouseY, detailX, detailY, 64, BUTTON_HEIGHT));

        int imageAreaX = detailX + 10;
        int imageAreaY = detailY + BUTTON_HEIGHT + 8;
        int imageAreaWidth = detailWidth - 20;
        int imageAreaHeight = detailHeight - BUTTON_HEIGHT - 18;

        drawDocumentImage(g, document, definition, imageAreaX, imageAreaY, imageAreaWidth, imageAreaHeight);
    }

    private void renderDocumentText(GuiGraphics g, int mouseX, int mouseY, ItemStack document, CodexDocumentDefinition definition) {
        int buttonY = getButtonY();
        int textY = detailY + 4;
        String title = definition.getDisplayName(document);

        g.drawString(mc.font, title, detailX, textY, TEXT_WHITE, false);
        textY += 18;

        String body = readText(definition).orElseGet(() -> buildFallbackText(document, definition));
        int maxTextY = buttonY - 8;
        for (FormattedCharSequence line : mc.font.split(Component.literal(body), detailWidth)) {
            if (textY + 10 > maxTextY) {
                g.drawString(mc.font, "...", detailX, textY, TEXT_GRAY, false);
                break;
            }
            g.drawString(mc.font, line, detailX, textY, TEXT_GRAY, false);
            textY += 12;
        }

        drawDetailButtons(g, mouseX, mouseY);
    }

    private void drawDocumentImage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition, int areaX, int areaY, int areaWidth, int areaHeight) {
        ResourceLocation image = definition.getImageLocation().orElse(null);
        if (image != null) {
            int[] fitted = fitRect(definition.getImageWidth(), definition.getImageHeight(), areaWidth, areaHeight);
            int imageX = areaX + (areaWidth - fitted[0]) / 2;
            int imageY = areaY + (areaHeight - fitted[1]) / 2;

            setTextureFiltering(image, true);
            g.blit(image, imageX, imageY, fitted[0], fitted[1], 0.0F, 0.0F, definition.getImageWidth(), definition.getImageHeight(), definition.getImageWidth(), definition.getImageHeight());
            setTextureFiltering(image, false);
            return;
        }

        renderDocumentPlaceholder(g, document, definition, areaX, areaY, areaWidth, areaHeight);
    }

    private void renderDocumentPlaceholder(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition, int areaX, int areaY, int areaWidth, int areaHeight) {
        int centerX = areaX + (areaWidth / 2);
        int centerY = areaY + (areaHeight / 2);
        int iconBox = 42;
        int iconX = centerX - (iconBox / 2);
        int iconY = centerY - 34;

        g.fill(iconX, iconY, iconX + iconBox, iconY + iconBox, ICON_BOX);
        g.pose().pushPose();
        g.pose().translate(iconX + 5, iconY + 5, 0.0F);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.renderItem(document, 0, 0);
        g.pose().popPose();

        String title = definition.getDisplayName(document);
        g.drawString(mc.font, title, centerX - (mc.font.width(title) / 2), iconY + iconBox + 10, TEXT_WHITE, false);
        String hint = "No image configured.";
        g.drawString(mc.font, hint, centerX - (mc.font.width(hint) / 2), iconY + iconBox + 23, TEXT_GRAY, false);
    }

    private void drawDetailButtons(GuiGraphics g, int mouseX, int mouseY) {
        int buttonY = getButtonY();
        int gap = 6;
        int buttonWidth = (detailWidth - gap) / 2;

        drawButton(g, detailX, buttonY, buttonWidth, BUTTON_HEIGHT, showingText ? "Show Document Image" : "Show Document as Text", clickedTextButton(mouseX, mouseY));
        drawButton(g, detailX + buttonWidth + gap, buttonY, buttonWidth, BUTTON_HEIGHT, "Expand Image", clickedExpandButton(mouseX, mouseY));
    }

    private void drawButton(GuiGraphics g, int x, int y, int width, int height, String label, boolean hovered) {
        g.fill(x, y, x + width, y + height, hovered ? BUTTON_BACKGROUND_HOVERED : BUTTON_BACKGROUND);
        g.drawString(mc.font, label, x + (width - mc.font.width(label)) / 2, y + 6, TEXT_WHITE, false);
    }

    private List<DisplayRow> buildRows() {
        Map<String, List<DisplayRow>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < documents.size(); i++) {
            ItemStack document = documents.get(i);
            if (document == null || document.isEmpty()) {
                continue;
            }

            CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(document);
            String category = definition.getCategory();
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>())
                    .add(DisplayRow.document(category, i, definition.getDisplayName(document)));
        }

        List<DisplayRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<DisplayRow>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<DisplayRow> docs = entry.getValue();
            docs.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));

            rows.add(DisplayRow.category(category));
            if (!collapsedCategories.contains(category)) {
                rows.addAll(docs);
            }
        }

        return rows;
    }

    private DisplayRow getClickedRow(double mouseX, double mouseY) {
        if (!isMouseOverList(mouseX, mouseY)) {
            return null;
        }

        int row = (int) ((mouseY - y) / ROW_HEIGHT);
        if (row < 0 || row >= getVisibleRows()) {
            return null;
        }

        int index = scrollOffset + row;
        List<DisplayRow> rows = buildRows();
        if (index < 0 || index >= rows.size()) {
            return null;
        }

        return rows.get(index);
    }

    private boolean isValidSelectedDocument() {
        return selectedIndex >= 0 && selectedIndex < documents.size() && !documents.get(selectedIndex).isEmpty();
    }

    private void normalizeSelection() {
        if (selectedIndex >= documents.size()) {
            selectedIndex = documents.isEmpty() ? -1 : documents.size() - 1;
            showingText = false;
            expandedImage = false;
        }
    }

    private int getVisibleRows() {
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private void clampScroll(int totalRows) {
        int max = Math.max(0, totalRows - getVisibleRows());
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= x
                && mouseX <= x + listWidth
                && mouseY >= y
                && mouseY <= y + listHeight;
    }

    private void renderScrollbar(GuiGraphics g, int totalRows) {
        if (totalRows <= getVisibleRows()) {
            return;
        }

        int trackX = x + listWidth - 8;
        int trackY = y;
        int trackHeight = getVisibleRows() * ROW_HEIGHT;
        int thumbHeight = Math.max(18, trackHeight * getVisibleRows() / totalRows);
        int maxScroll = Math.max(1, totalRows - getVisibleRows());
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + (thumbTravel * scrollOffset / maxScroll);

        g.fill(trackX, trackY, trackX + SCROLL_WIDTH, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + SCROLL_WIDTH, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private Optional<String> readText(CodexDocumentDefinition definition) {
        ResourceLocation textLocation = definition.getTextLocation().orElse(null);
        if (textLocation == null || mc == null) {
            return Optional.empty();
        }

        return mc.getResourceManager().getResource(textLocation).flatMap(resource -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(line);
                }
                return Optional.of(builder.toString());
            } catch (IOException ignored) {
                return Optional.empty();
            }
        });
    }

    private String buildFallbackText(ItemStack document, CodexDocumentDefinition definition) {
        StringBuilder builder = new StringBuilder();
        builder.append(definition.getDisplayName(document)).append("\n\n");
        builder.append("No transcription file configured for this document.");
        if (document.hasTag() && document.getTag() != null) {
            builder.append("\n\nNBT:\n").append(document.getTag());
        }
        return builder.toString();
    }

    private int getButtonY() {
        return detailY + detailHeight - BUTTON_HEIGHT - 6;
    }

    private boolean clickedTextButton(double mouseX, double mouseY) {
        int gap = 6;
        int buttonWidth = (detailWidth - gap) / 2;
        return isMouseInside(mouseX, mouseY, detailX, getButtonY(), buttonWidth, BUTTON_HEIGHT);
    }

    private boolean clickedExpandButton(double mouseX, double mouseY) {
        int gap = 6;
        int buttonWidth = (detailWidth - gap) / 2;
        return isMouseInside(mouseX, mouseY, detailX + buttonWidth + gap, getButtonY(), buttonWidth, BUTTON_HEIGHT);
    }

    private boolean clickedReturnButton(double mouseX, double mouseY) {
        return isMouseInside(mouseX, mouseY, detailX, detailY, 64, BUTTON_HEIGHT);
    }

    private boolean isMouseInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int[] fitRect(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        float scale = Math.min(maxWidth / (float) sourceWidth, maxHeight / (float) sourceHeight);
        int width = Math.max(1, Math.round(sourceWidth * scale));
        int height = Math.max(1, Math.round(sourceHeight * scale));
        return new int[]{width, height};
    }

    private void setTextureFiltering(ResourceLocation texture, boolean blur) {
        if (mc == null) return;
        mc.getTextureManager().getTexture(texture).setFilter(blur, false);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://CODEX_";
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false);
    }

    private static class DisplayRow {
        private final boolean categoryRow;
        private final String category;
        private final int documentIndex;
        private final String name;

        private DisplayRow(boolean categoryRow, String category, int documentIndex, String name) {
            this.categoryRow = categoryRow;
            this.category = category;
            this.documentIndex = documentIndex;
            this.name = name;
        }

        private static DisplayRow category(String category) {
            return new DisplayRow(true, category, -1, category);
        }

        private static DisplayRow document(String category, int documentIndex, String name) {
            return new DisplayRow(false, category, documentIndex, name);
        }
    }
}
