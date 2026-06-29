package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class CodexPanel {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_BODY = 0xF2B2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TEXT_SELECTED = 0xFF202020;
    private static final int CATEGORY_BACKGROUND = 0x3E6A6C6C;
    private static final int SELECTED_BACKGROUND = 0x889FC8C8;
    private static final int BUTTON_BACKGROUND = 0x446A6C6C;
    private static final int BUTTON_BACKGROUND_HOVERED = 0x667A7C7C;
    private static final int SCROLL_TRACK = 0x33000000;
    private static final int SCROLL_THUMB = 0x886A6C6C;
    private static final int OVERLAY_BACKGROUND = 0xCC000000;
    private static final int DEBUG_PAGE = 0xE8D7D8D5;
    private static final int DEBUG_PAGE_DARK = 0xFF202020;
    private static final int DEBUG_PAGE_FAINT = 0x66303030;
    private static final int DEBUG_HIGHLIGHT = 0x99D8D24A;

    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 14;
    private static final int ZOOM_BUTTON_SIZE = 14;
    private static final int SCROLL_WIDTH = 5;
    private static final int LIST_CONTENT_Y_OFFSET = -16;
    private static final int DETAIL_PAD_X = 4;
    private static final int DETAIL_PAD_TOP = 0;
    private static final int DETAIL_PAD_BOTTOM = 4;
    private static final int IMAGE_BOTTOM_GAP = 8;
    private static final int TEXT_TOP_GAP = 5;

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
    private int textScrollOffset = 0;
    private int textZoomLevel = 0;
    private boolean showingText = false;
    private boolean expandedImage = false;

    public CodexPanel(int x, int y, int listWidth, int detailX, int detailWidth, int titleY, int listTitleX, int detailTitleX, IScpInventory inventory) {
        this(
                x,
                y,
                listWidth,
                guessPanelHeight(y),
                detailX,
                detailWidth,
                guessPanelHeight(y),
                titleY,
                listTitleX,
                detailTitleX,
                inventory == null ? List.of() : inventory.getDocuments()
        );
    }

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
        this.documents = documents == null ? List.of() : documents;
    }

    public boolean isExpandedImage() {
        return expandedImage;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitle(g, listTitleX, titleY, "CLASSIFICATION");
        drawSectionTitle(g, detailTitleX, titleY, "DOCUMENT");
        renderDocumentList(g);
        renderDocumentDetails(g, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expandedImage) {
            expandedImage = false;
            return true;
        }

        if (button != 0) {
            return false;
        }

        if (isValidSelectedDocument()) {
            if (showingText) {
                if (clickedReturnButton(mouseX, mouseY)) {
                    showingText = false;
                    textScrollOffset = 0;
                    return true;
                }

                if (clickedZoomInButton(mouseX, mouseY)) {
                    textZoomLevel = Math.min(5, textZoomLevel + 1);
                    textScrollOffset = 0;
                    return true;
                }

                if (clickedZoomOutButton(mouseX, mouseY)) {
                    textZoomLevel = Math.max(-2, textZoomLevel - 1);
                    textScrollOffset = 0;
                    return true;
                }
            }

            if (!showingText && clickedTextButton(mouseX, mouseY)) {
                showingText = true;
                expandedImage = false;
                textScrollOffset = 0;
                return true;
            }

            if (!showingText && clickedExpandButton(mouseX, mouseY)) {
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
        textScrollOffset = 0;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showingText && isMouseOverDetail(mouseX, mouseY) && isValidSelectedDocument()) {
            if (delta < 0) textScrollOffset += 3;
            if (delta > 0) textScrollOffset -= 3;
            clampTextScroll();
            return true;
        }

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
        int listContentY = getListContentY();
        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scrollOffset + i;
            if (rowIndex >= rows.size()) break;

            DisplayRow row = rows.get(rowIndex);
            int rowY = listContentY + (i * ROW_HEIGHT);
            if (row.categoryRow) {
                renderCategoryRow(g, row, rowY);
            } else {
                renderDocumentRow(g, row, rowY);
            }
        }

        renderScrollbar(g, rows.size());
    }

    private void renderCategoryRow(GuiGraphics g, DisplayRow row, int rowY) {
        g.fill(x - 1, rowY, x + listWidth - 22, rowY + ROW_HEIGHT, CATEGORY_BACKGROUND);
        g.drawString(mc.font, row.category, x + 12, rowY + 7, TEXT_WHITE, false);
        g.drawString(mc.font, collapsedCategories.contains(row.category) ? ">" : "v", x + listWidth - 44, rowY + 7, TEXT_GRAY, false);
    }

    private void renderDocumentRow(GuiGraphics g, DisplayRow row, int rowY) {
        boolean selected = row.documentIndex == selectedIndex;
        if (selected) {
            g.fill(x + 6, rowY, x + listWidth - 36, rowY + ROW_HEIGHT, SELECTED_BACKGROUND);
        }
        g.drawString(mc.font, row.name, x + 26, rowY + 7, selected ? TEXT_SELECTED : TEXT_WHITE, false);
    }

    private void renderDocumentDetails(GuiGraphics g, int mouseX, int mouseY) {
        if (!isValidSelectedDocument()) return;

        ItemStack document = documents.get(selectedIndex);
        CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(document);

        if (showingText) {
            renderDocumentText(g, mouseX, mouseY, document, definition);
        } else {
            renderDocumentImagePreview(g, mouseX, mouseY, document, definition);
        }

        if (expandedImage) {
            renderExpandedImage(g, document, definition);
        }
    }

    private void renderDocumentImagePreview(GuiGraphics g, int mouseX, int mouseY, ItemStack document, CodexDocumentDefinition definition) {
        int buttonY = getButtonY();
        int imageAreaX = getDetailLeft() + 4;
        int imageAreaY = getDetailTop();
        int imageAreaWidth = getDetailRight() - imageAreaX - 4;
        int imageAreaHeight = Math.max(40, buttonY - imageAreaY - IMAGE_BOTTOM_GAP);

        drawDocumentImage(g, document, definition, imageAreaX, imageAreaY, imageAreaWidth, imageAreaHeight);
        drawDetailButtons(g, mouseX, mouseY);
    }

    private void renderExpandedImage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        g.fill(0, 0, screenWidth, screenHeight, OVERLAY_BACKGROUND);

        int marginX = Math.max(18, screenWidth / 16);
        int marginY = Math.max(12, screenHeight / 24);
        drawDocumentImage(
                g,
                document,
                definition,
                marginX,
                marginY,
                screenWidth - marginX * 2,
                screenHeight - marginY * 2
        );
    }

    private void renderDocumentText(GuiGraphics g, int mouseX, int mouseY, ItemStack document, CodexDocumentDefinition definition) {
        drawTopControls(g, mouseX, mouseY, definition.getDisplayName(document));

        int textX = getDetailLeft() + 2;
        int textY = getControlY() + BUTTON_HEIGHT + TEXT_TOP_GAP;
        int textRight = getDetailRight() - SCROLL_WIDTH - 6;
        int textWidth = Math.max(40, textRight - textX);
        int textHeight = Math.max(40, getDetailBottom() - textY);
        float scale = getTextScale();
        int scaledTextWidth = Math.max(40, (int) (textWidth / scale));
        int lineHeight = Math.max(8, Math.round(12 * scale));

        String body = readText(definition).orElseGet(() -> buildFallbackText(document, definition));
        List<TextLine> lines = wrapJustifiedText(body, scaledTextWidth);
        int visibleLines = Math.max(1, textHeight / lineHeight);
        clampTextScroll(lines.size(), visibleLines);

        g.enableScissor(textX, textY, textX + textWidth, textY + textHeight);
        int drawY = textY;
        for (int i = 0; i < visibleLines + 2; i++) {
            int index = textScrollOffset + i;
            if (index >= lines.size()) break;
            drawTextLine(g, lines.get(index), textX, drawY, TEXT_BODY, scale, scaledTextWidth);
            drawY += lineHeight;
        }
        g.disableScissor();

        renderTextScrollbar(g, lines.size(), visibleLines, getDetailRight() - SCROLL_WIDTH, textY, textHeight);
    }

    private void drawDocumentImage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition, int areaX, int areaY, int areaWidth, int areaHeight) {
        ResourceLocation image = definition.getImageLocation().orElse(null);
        if (image != null) {
            int[] fitted = fitRect(definition.getImageWidth(), definition.getImageHeight(), areaWidth, areaHeight);
            int fittedWidth = Math.max(1, fitted[0]);
            int fittedHeight = Math.max(1, fitted[1]);
            int imageX = areaX + (areaWidth - fittedWidth) / 2;
            int imageY = areaY + (areaHeight - fittedHeight) / 2;

            g.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
            setTextureFiltering(image, true);
            g.blit(image, imageX, imageY, fittedWidth, fittedHeight, 0.0F, 0.0F, definition.getImageWidth(), definition.getImageHeight(), definition.getImageWidth(), definition.getImageHeight());
            setTextureFiltering(image, false);
            g.disableScissor();
            return;
        }

        renderDebugDocumentPage(g, document, definition, areaX, areaY, areaWidth, areaHeight);
    }

    private void renderDebugDocumentPage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition, int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] fitted = fitRect(1279, 1920, areaWidth, areaHeight);
        int pageW = Math.max(1, fitted[0]);
        int pageH = Math.max(1, fitted[1]);
        int pageX = areaX + (areaWidth - pageW) / 2;
        int pageY = areaY + (areaHeight - pageH) / 2;

        g.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
        g.fill(pageX, pageY, pageX + pageW, pageY + pageH, DEBUG_PAGE);
        g.fill(pageX + pageW / 22, pageY + pageH / 8, pageX + pageW - pageW / 22, pageY + pageH / 8 + 1, DEBUG_PAGE_DARK);

        float titleScale = Math.max(0.65F, pageW / 360.0F);
        drawScaledPageString(g, "SCP", pageX + pageW / 12.0F, pageY + pageH / 22.0F, DEBUG_PAGE_DARK, titleScale * 2.2F);
        drawScaledPageString(g, "Secure. Contain. Protect.", pageX + pageW / 12.0F, pageY + pageH / 10.0F, DEBUG_PAGE_DARK, titleScale * 0.72F);

        String documentTitle = definition.getDisplayName(document);
        drawScaledPageString(g, documentTitle, pageX + pageW / 12.0F, pageY + pageH / 6.2F, DEBUG_PAGE_DARK, titleScale * 0.72F);
        drawScaledPageString(g, "Special Containment Procedures:", pageX + pageW / 12.0F, pageY + pageH / 4.7F, DEBUG_PAGE_DARK, titleScale * 0.62F);

        int lineX = pageX + pageW / 12;
        int lineW = pageW - pageW / 6;
        int lineY = pageY + pageH / 4;
        int lineStep = Math.max(5, pageH / 42);
        for (int i = 0; i < 19; i++) {
            int currentLineW = lineW - ((i % 4) * pageW / 18);
            g.fill(lineX, lineY + i * lineStep, lineX + currentLineW, lineY + i * lineStep + 1, DEBUG_PAGE_FAINT);
        }

        int highlightY = lineY + lineStep * 7;
        g.fill(lineX, highlightY - 1, lineX + lineW - pageW / 10, highlightY + lineStep + 1, DEBUG_HIGHLIGHT);
        g.fill(lineX, highlightY + lineStep + 2, lineX + lineW - pageW / 5, highlightY + (lineStep * 2) + 4, DEBUG_HIGHLIGHT);

        drawScaledPageString(g, "This is a generated debug preview.", lineX, pageY + pageH - pageH / 7.5F, DEBUG_PAGE_DARK, titleScale * 0.60F);
        drawScaledPageString(g, "Use the text button for the transcript.", lineX, pageY + pageH - pageH / 9.0F, DEBUG_PAGE_DARK, titleScale * 0.60F);
        g.disableScissor();
    }

    private void drawTopControls(GuiGraphics g, int mouseX, int mouseY, String title) {
        int controlY = getControlY();
        int returnX = getDetailLeft();
        int returnW = 58;
        int zoomMinusX = getDetailRight() - ZOOM_BUTTON_SIZE;
        int zoomPlusX = zoomMinusX - ZOOM_BUTTON_SIZE - 5;

        drawButton(g, returnX, controlY, returnW, BUTTON_HEIGHT, "Return", clickedReturnButton(mouseX, mouseY));
        drawButton(g, zoomPlusX, controlY, ZOOM_BUTTON_SIZE, BUTTON_HEIGHT, "+", clickedZoomInButton(mouseX, mouseY));
        drawButton(g, zoomMinusX, controlY, ZOOM_BUTTON_SIZE, BUTTON_HEIGHT, "-", clickedZoomOutButton(mouseX, mouseY));

        int titleX = returnX + returnW + 8;
        int titleRight = zoomPlusX - 8;
        int titleWidth = Math.max(20, titleRight - titleX);
        int drawX = titleX + Math.max(0, (titleWidth - mc.font.width(title)) / 2);
        g.enableScissor(titleX, controlY - 2, titleRight, controlY + BUTTON_HEIGHT + 5);
        g.drawString(mc.font, title, drawX, controlY + 3, TEXT_WHITE, false);
        g.disableScissor();
    }

    private void drawDetailButtons(GuiGraphics g, int mouseX, int mouseY) {
        int buttonY = getButtonY();
        int gap = 6;
        int buttonX = getDetailLeft();
        int availableWidth = getDetailRight() - buttonX;
        int buttonWidth = (availableWidth - gap) / 2;

        drawButton(g, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, "Show Document as Text", clickedTextButton(mouseX, mouseY));
        drawButton(g, buttonX + buttonWidth + gap, buttonY, buttonWidth, BUTTON_HEIGHT, "Expand Document", clickedExpandButton(mouseX, mouseY));
    }

    private void drawButton(GuiGraphics g, int x, int y, int width, int height, String label, boolean hovered) {
        g.fill(x, y, x + width, y + height, hovered ? BUTTON_BACKGROUND_HOVERED : BUTTON_BACKGROUND);
        g.drawString(mc.font, label, x + (width - mc.font.width(label)) / 2, y + 3, TEXT_WHITE, false);
    }

    private void drawScaledPageString(GuiGraphics g, String text, float x, float y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private void drawTextLine(GuiGraphics g, TextLine line, int x, int y, int color, float scale, int targetWidth) {
        if (!line.justify || line.text.isBlank()) {
            drawScaledString(g, line.text, x, y, color, scale);
            return;
        }

        String[] words = line.text.trim().split("\\s+");
        if (words.length <= 2) {
            drawScaledString(g, line.text, x, y, color, scale);
            return;
        }

        int wordWidth = 0;
        for (String word : words) {
            wordWidth += mc.font.width(word);
        }

        int gaps = words.length - 1;
        int baseSpace = mc.font.width(" ");
        int extra = targetWidth - wordWidth - (baseSpace * gaps);
        if (extra <= 0 || extra > targetWidth / 2) {
            drawScaledString(g, line.text, x, y, color, scale);
            return;
        }

        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        float cursorX = 0.0F;
        float gapWidth = baseSpace + (extra / (float) gaps);
        for (String word : words) {
            g.drawString(mc.font, word, Math.round(cursorX), 0, color, false);
            cursorX += mc.font.width(word) + gapWidth;
        }
        g.pose().popPose();
    }

    private void drawScaledString(GuiGraphics g, String text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private List<DisplayRow> buildRows() {
        Map<String, List<DisplayRow>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < documents.size(); i++) {
            ItemStack document = documents.get(i);
            if (document == null || document.isEmpty()) continue;

            CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(document);
            grouped.computeIfAbsent(definition.getCategory(), ignored -> new ArrayList<>())
                    .add(DisplayRow.document(definition.getCategory(), i, definition.getDisplayName(document)));
        }

        List<DisplayRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<DisplayRow>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<DisplayRow> docs = entry.getValue();
            docs.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));
            rows.add(DisplayRow.category(category));
            if (!collapsedCategories.contains(category)) rows.addAll(docs);
        }
        return rows;
    }

    private List<TextLine> wrapJustifiedText(String body, int width) {
        List<TextLine> lines = new ArrayList<>();
        List<String> paragraphs = splitIntoParagraphs(body);
        for (int p = 0; p < paragraphs.size(); p++) {
            String paragraph = paragraphs.get(p);
            if (paragraph.isBlank()) {
                lines.add(new TextLine("", false));
                continue;
            }

            List<String> paragraphLines = wrapParagraph(paragraph, width);
            for (int i = 0; i < paragraphLines.size(); i++) {
                lines.add(new TextLine(paragraphLines.get(i), i < paragraphLines.size() - 1));
            }

            if (p < paragraphs.size() - 1) {
                lines.add(new TextLine("", false));
            }
        }
        return lines;
    }

    private List<String> splitIntoParagraphs(String body) {
        String normalized = body.replace("\r\n", "\n").replace('\r', '\n');
        String[] chunks = normalized.split("\\n\\s*\\n", -1);
        List<String> paragraphs = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.isBlank()) {
                paragraphs.add("");
            } else {
                paragraphs.add(chunk.replaceAll("\\n+", " ").trim());
            }
        }
        return paragraphs;
    }

    private List<String> wrapParagraph(String paragraph, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = paragraph.trim().split("\\s+");
        String current = "";

        for (String word : words) {
            if (current.isBlank()) {
                current = word;
                continue;
            }

            String candidate = current + " " + word;
            if (mc.font.width(candidate) <= width) {
                current = candidate;
            } else {
                lines.add(current);
                current = word;
            }
        }

        if (!current.isBlank()) lines.add(current);
        return lines.isEmpty() ? List.of("") : lines;
    }

    private DisplayRow getClickedRow(double mouseX, double mouseY) {
        if (!isMouseOverList(mouseX, mouseY)) return null;

        int listContentY = getListContentY();
        int row = (int) ((mouseY - listContentY) / ROW_HEIGHT);
        if (row < 0 || row >= getVisibleRows()) return null;

        int index = scrollOffset + row;
        List<DisplayRow> rows = buildRows();
        if (index < 0 || index >= rows.size()) return null;
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
            textScrollOffset = 0;
        }
    }

    private int getVisibleRows() {
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private int getListContentY() {
        return y + LIST_CONTENT_Y_OFFSET;
    }

    private void clampScroll(int totalRows) {
        int max = Math.max(0, totalRows - getVisibleRows());
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    private void clampTextScroll() {
        if (!isValidSelectedDocument()) {
            textScrollOffset = 0;
            return;
        }
        CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(documents.get(selectedIndex));
        String body = readText(definition).orElseGet(() -> buildFallbackText(documents.get(selectedIndex), definition));
        float scale = getTextScale();
        int textWidth = Math.max(40, getDetailRight() - SCROLL_WIDTH - 6 - (getDetailLeft() + 2));
        int scaledTextWidth = Math.max(40, (int) (textWidth / scale));
        int textHeight = Math.max(40, getDetailBottom() - (getControlY() + BUTTON_HEIGHT + TEXT_TOP_GAP));
        int visibleLines = Math.max(1, textHeight / Math.max(8, Math.round(12 * scale)));
        clampTextScroll(wrapJustifiedText(body, scaledTextWidth).size(), visibleLines);
    }

    private void clampTextScroll(int totalLines, int visibleLines) {
        int max = Math.max(0, totalLines - visibleLines);
        if (textScrollOffset < 0) textScrollOffset = 0;
        if (textScrollOffset > max) textScrollOffset = max;
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        int listContentY = getListContentY();
        return mouseX >= x - 1
                && mouseX <= x + listWidth - 16
                && mouseY >= listContentY
                && mouseY <= listContentY + listHeight;
    }

    private boolean isMouseOverDetail(double mouseX, double mouseY) {
        return mouseX >= getDetailLeft()
                && mouseX <= getDetailRight()
                && mouseY >= getDetailTop()
                && mouseY <= getDetailBottom();
    }

    private void renderScrollbar(GuiGraphics g, int totalRows) {
        if (totalRows <= getVisibleRows()) return;

        int trackX = x + listWidth - 14;
        int trackY = getListContentY();
        int trackHeight = getVisibleRows() * ROW_HEIGHT;
        int thumbHeight = Math.max(18, trackHeight * getVisibleRows() / totalRows);
        int maxScroll = Math.max(1, totalRows - getVisibleRows());
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + (thumbTravel * scrollOffset / maxScroll);

        g.fill(trackX, trackY, trackX + SCROLL_WIDTH, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + SCROLL_WIDTH, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private void renderTextScrollbar(GuiGraphics g, int totalLines, int visibleLines, int trackX, int trackY, int trackHeight) {
        if (totalLines <= visibleLines) return;

        int thumbHeight = Math.max(18, trackHeight * visibleLines / totalLines);
        int maxScroll = Math.max(1, totalLines - visibleLines);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + (thumbTravel * textScrollOffset / maxScroll);

        g.fill(trackX, trackY, trackX + SCROLL_WIDTH, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + SCROLL_WIDTH, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private Optional<String> readText(CodexDocumentDefinition definition) {
        ResourceLocation textLocation = definition.getTextLocation().orElse(null);
        if (textLocation == null || mc == null) return Optional.empty();

        return mc.getResourceManager().getResource(textLocation).flatMap(resource -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (builder.length() > 0) builder.append('\n');
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
        return getDetailBottom() - BUTTON_HEIGHT;
    }

    private int getEffectiveDetailHeight() {
        return Math.max(80, detailHeight);
    }

    private int getDetailLeft() {
        return detailX + DETAIL_PAD_X;
    }

    private int getDetailRight() {
        return detailX + detailWidth - DETAIL_PAD_X;
    }

    private int getDetailTop() {
        return detailY + DETAIL_PAD_TOP;
    }

    private int getDetailBottom() {
        return detailY + getEffectiveDetailHeight() - DETAIL_PAD_BOTTOM;
    }

    private int getControlY() {
        return getDetailTop() - 14;
    }

    private boolean clickedTextButton(double mouseX, double mouseY) {
        int gap = 6;
        int buttonX = getDetailLeft();
        int buttonWidth = (getDetailRight() - buttonX - gap) / 2;
        return isMouseInside(mouseX, mouseY, buttonX, getButtonY(), buttonWidth, BUTTON_HEIGHT);
    }

    private boolean clickedExpandButton(double mouseX, double mouseY) {
        int gap = 6;
        int buttonX = getDetailLeft();
        int buttonWidth = (getDetailRight() - buttonX - gap) / 2;
        return isMouseInside(mouseX, mouseY, buttonX + buttonWidth + gap, getButtonY(), buttonWidth, BUTTON_HEIGHT);
    }

    private boolean clickedReturnButton(double mouseX, double mouseY) {
        return isMouseInside(mouseX, mouseY, getDetailLeft(), getControlY(), 58, BUTTON_HEIGHT);
    }

    private boolean clickedZoomInButton(double mouseX, double mouseY) {
        int zoomMinusX = getDetailRight() - ZOOM_BUTTON_SIZE;
        int zoomPlusX = zoomMinusX - ZOOM_BUTTON_SIZE - 5;
        return isMouseInside(mouseX, mouseY, zoomPlusX, getControlY(), ZOOM_BUTTON_SIZE, BUTTON_HEIGHT);
    }

    private boolean clickedZoomOutButton(double mouseX, double mouseY) {
        return isMouseInside(mouseX, mouseY, getDetailRight() - ZOOM_BUTTON_SIZE, getControlY(), ZOOM_BUTTON_SIZE, BUTTON_HEIGHT);
    }

    private boolean isMouseInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private float getTextScale() {
        return Math.max(0.68F, 0.88F + (textZoomLevel * 0.12F));
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

    private static int guessPanelHeight(int panelY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) return ROW_HEIGHT * 12;
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        return Math.max(ROW_HEIGHT * 9, guiHeight - panelY - 160);
    }

    private static class TextLine {
        private final String text;
        private final boolean justify;

        private TextLine(String text, boolean justify) {
            this.text = text;
            this.justify = justify;
        }
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
