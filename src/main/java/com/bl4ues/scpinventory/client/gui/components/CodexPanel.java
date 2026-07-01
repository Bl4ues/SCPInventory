package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.client.ClientInventoryBridge;
import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.DocumentActionPacket;
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
    private static final int DRAG_ICON_BOX = 0x99303638;
    private static final int DRAG_ICON_CORNER = 0xCC6A6C6C;

    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 14;
    private static final int SCROLL_WIDTH = 5;
    private static final int LIST_CONTENT_Y_OFFSET = -16;
    private static final int DETAIL_PAD_X = 4;
    private static final int DETAIL_PAD_BOTTOM = 4;
    private static final int DRAG_ICON_FRAME_SIZE = 24;
    private static final double DRAG_THRESHOLD = 4.0D;

    private final Minecraft mc = Minecraft.getInstance();
    private final ContextMenu contextMenu = new ContextMenu();
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
    private int contextDocumentIndex = -1;
    private int draggedDocumentIndex = -1;
    private double dragStartX = 0.0D;
    private double dragStartY = 0.0D;
    private double lastDragX = 0.0D;
    private double lastDragY = 0.0D;
    private boolean showingText = false;
    private boolean expandedImage = false;
    private boolean draggingListScrollbar = false;
    private boolean documentDragMoved = false;
    private ItemStack draggedDocumentStack = ItemStack.EMPTY;

    public CodexPanel(int x, int y, int listWidth, int detailX, int detailWidth, int titleY, int listTitleX, int detailTitleX, IScpInventory inventory) {
        this(x, y, listWidth, guessPanelHeight(y), detailX, detailWidth, guessPanelHeight(y), titleY, listTitleX, detailTitleX,
                inventory == null ? List.of() : inventory.getDocuments());
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
        if (!expandedImage) {
            contextMenu.render(g, mouseX, mouseY);
            renderDraggedDocument(g, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expandedImage) {
            expandedImage = false;
            return true;
        }

        if (contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option != -1) {
                if (contextDocumentIndex >= 0) ClientInventoryBridge.performDocument(contextDocumentIndex, contextMenu.getOption(option));
                contextMenu.close();
                contextDocumentIndex = -1;
                return true;
            }
            contextMenu.close();
            contextDocumentIndex = -1;
        }

        if (button == 0 && clickedListScrollbar(mouseX, mouseY)) {
            draggingListScrollbar = true;
            updateListScrollFromMouse(mouseY);
            return true;
        }

        if (button == 0 && handleDetailClick(mouseX, mouseY)) return true;

        DisplayRow clickedRow = getClickedRow(mouseX, mouseY);
        if (clickedRow == null) return false;

        if (clickedRow.categoryRow) {
            if (button != 0) return false;
            if (collapsedCategories.contains(clickedRow.category)) collapsedCategories.remove(clickedRow.category);
            else collapsedCategories.add(clickedRow.category);
            clampScroll(buildRows().size());
            return true;
        }

        if (button == 1) {
            contextDocumentIndex = clickedRow.documentIndex;
            contextMenu.open((int) mouseX, (int) mouseY, "Document");
            return true;
        }

        if (button == 0) {
            selectDocument(clickedRow.documentIndex);
            startDocumentDrag(clickedRow.documentIndex, mouseX, mouseY);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return false;

        if (draggingListScrollbar) {
            updateListScrollFromMouse(mouseY);
            return true;
        }

        if (draggedDocumentIndex >= 0 && !draggedDocumentStack.isEmpty()) {
            lastDragX = mouseX;
            lastDragY = mouseY;
            if (Math.abs(mouseX - dragStartX) > DRAG_THRESHOLD || Math.abs(mouseY - dragStartY) > DRAG_THRESHOLD) documentDragMoved = true;
            return true;
        }

        return false;
    }

    public boolean mouseReleased(int button) {
        if (button == 0 && draggedDocumentIndex >= 0) {
            if (documentDragMoved && !isInsideCodexPanels(lastDragX, lastDragY)) {
                ClientInventoryBridge.performDocument(draggedDocumentIndex, DocumentActionPacket.ACTION_DROP);
            }
            clearDocumentDrag();
            return true;
        }

        if (button != 0 || !draggingListScrollbar) return false;
        draggingListScrollbar = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showingText && isMouseOverDetail(mouseX, mouseY) && isValidSelectedDocument()) {
            if (delta < 0) textScrollOffset += 3;
            if (delta > 0) textScrollOffset -= 3;
            clampTextScroll();
            return true;
        }

        if (!isMouseOverList(mouseX, mouseY)) return false;
        if (delta < 0) scrollOffset++;
        if (delta > 0) scrollOffset--;
        clampScroll(buildRows().size());
        return true;
    }

    private boolean handleDetailClick(double mouseX, double mouseY) {
        if (!isValidSelectedDocument()) return false;

        if (showingText) {
            if (clickedReturnButton(mouseX, mouseY)) {
                showingText = false;
                textScrollOffset = 0;
                return true;
            }
        } else {
            if (clickedTextButton(mouseX, mouseY)) {
                showingText = true;
                expandedImage = false;
                textScrollOffset = 0;
                return true;
            }
            if (clickedExpandButton(mouseX, mouseY)) {
                expandedImage = true;
                showingText = false;
                return true;
            }
        }
        return false;
    }

    private void selectDocument(int index) {
        if (index < 0 || index >= documents.size() || documents.get(index).isEmpty()) return;
        selectedIndex = index;
        showingText = false;
        expandedImage = false;
        textScrollOffset = 0;
    }

    private void startDocumentDrag(int index, double mouseX, double mouseY) {
        if (index < 0 || index >= documents.size()) return;
        ItemStack stack = documents.get(index);
        if (stack.isEmpty()) return;
        draggedDocumentIndex = index;
        draggedDocumentStack = stack.copy();
        dragStartX = mouseX;
        dragStartY = mouseY;
        lastDragX = mouseX;
        lastDragY = mouseY;
        documentDragMoved = false;
        contextMenu.close();
    }

    private void clearDocumentDrag() {
        draggedDocumentIndex = -1;
        draggedDocumentStack = ItemStack.EMPTY;
        dragStartX = 0.0D;
        dragStartY = 0.0D;
        lastDragX = 0.0D;
        lastDragY = 0.0D;
        documentDragMoved = false;
    }

    private void renderDraggedDocument(GuiGraphics g, int mouseX, int mouseY) {
        if (draggedDocumentStack.isEmpty() || !documentDragMoved) return;
        int frameX = mouseX - (DRAG_ICON_FRAME_SIZE / 2);
        int frameY = mouseY - (DRAG_ICON_FRAME_SIZE / 2);
        drawDragIconFrame(g, frameX, frameY);
        g.renderItem(draggedDocumentStack, frameX + 4, frameY + 4);
    }

    private void drawDragIconFrame(GuiGraphics g, int x, int y) {
        int right = x + DRAG_ICON_FRAME_SIZE;
        int bottom = y + DRAG_ICON_FRAME_SIZE;
        int corner = 6;
        g.fill(x, y, right, bottom, DRAG_ICON_BOX);
        g.fill(x, y, x + corner, y + 1, DRAG_ICON_CORNER);
        g.fill(x, y, x + 1, y + corner, DRAG_ICON_CORNER);
        g.fill(right - corner, y, right, y + 1, DRAG_ICON_CORNER);
        g.fill(right - 1, y, right, y + corner, DRAG_ICON_CORNER);
        g.fill(x, bottom - 1, x + corner, bottom, DRAG_ICON_CORNER);
        g.fill(x, bottom - corner, x + 1, bottom, DRAG_ICON_CORNER);
        g.fill(right - corner, bottom - 1, right, bottom, DRAG_ICON_CORNER);
        g.fill(right - 1, bottom - corner, right, bottom, DRAG_ICON_CORNER);
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
            if (row.categoryRow) renderCategoryRow(g, row, rowY);
            else renderDocumentRow(g, row, rowY);
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
        if (selected) g.fill(x + 6, rowY, x + listWidth - 36, rowY + ROW_HEIGHT, SELECTED_BACKGROUND);
        g.drawString(mc.font, row.name, x + 26, rowY + 7, selected ? TEXT_SELECTED : TEXT_WHITE, false);
    }

    private void renderDocumentDetails(GuiGraphics g, int mouseX, int mouseY) {
        if (!isValidSelectedDocument()) return;
        ItemStack document = documents.get(selectedIndex);
        CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(document);
        if (showingText) renderDocumentText(g, document, definition);
        else renderDocumentImagePreview(g, mouseX, mouseY, document, definition);
        if (expandedImage) renderExpandedImage(g, document, definition);
    }

    private void renderDocumentImagePreview(GuiGraphics g, int mouseX, int mouseY, ItemStack document, CodexDocumentDefinition definition) {
        int buttonY = getButtonY();
        int imageAreaX = getDetailLeft() + 4;
        int imageAreaY = getDetailTop();
        int imageAreaWidth = getDetailRight() - imageAreaX - 4;
        int imageAreaHeight = Math.max(40, buttonY - imageAreaY - 8);
        drawDocumentImage(g, document, definition, imageAreaX, imageAreaY, imageAreaWidth, imageAreaHeight);
        drawDetailButtons(g, mouseX, mouseY);
    }

    private void renderExpandedImage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        g.fill(0, 0, screenWidth, screenHeight, OVERLAY_BACKGROUND);
        int marginX = Math.max(18, screenWidth / 16);
        int marginY = Math.max(12, screenHeight / 24);
        drawDocumentImage(g, document, definition, marginX, marginY, screenWidth - marginX * 2, screenHeight - marginY * 2);
    }

    private void renderDocumentText(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition) {
        drawButton(g, getDetailLeft(), getControlY(), 58, BUTTON_HEIGHT, "Return", false);
        int textX = getDetailLeft() + 2;
        int textY = getControlY() + BUTTON_HEIGHT + 5;
        int textRight = getDetailRight() - SCROLL_WIDTH - 6;
        int textWidth = Math.max(40, textRight - textX);
        int textHeight = Math.max(40, getDetailBottom() - textY);
        int lineHeight = 11;
        String body = readText(definition).orElseGet(() -> buildFallbackText(document, definition));
        List<String> lines = wrapText(body, textWidth);
        int visibleLines = Math.max(1, textHeight / lineHeight);
        clampTextScroll(lines.size(), visibleLines);
        g.enableScissor(textX, textY, textX + textWidth, textY + textHeight);
        int drawY = textY;
        for (int i = 0; i < visibleLines + 2; i++) {
            int index = textScrollOffset + i;
            if (index >= lines.size()) break;
            g.drawString(mc.font, lines.get(index), textX, drawY, TEXT_BODY, false);
            drawY += lineHeight;
        }
        g.disableScissor();
        renderTextScrollbar(g, lines.size(), visibleLines, getDetailRight() - SCROLL_WIDTH, textY, textHeight);
    }

    private void drawDocumentImage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition, int areaX, int areaY, int areaWidth, int areaHeight) {
        ResourceLocation image = definition.getImageLocation().orElse(null);
        if (image != null) {
            int[] fitted = fitRect(definition.getImageWidth(), definition.getImageHeight(), areaWidth, areaHeight);
            int imageX = areaX + (areaWidth - fitted[0]) / 2;
            int imageY = areaY + (areaHeight - fitted[1]) / 2;
            g.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
            setTextureFiltering(image, true);
            g.blit(image, imageX, imageY, fitted[0], fitted[1], 0.0F, 0.0F, definition.getImageWidth(), definition.getImageHeight(), definition.getImageWidth(), definition.getImageHeight());
            setTextureFiltering(image, false);
            g.disableScissor();
            return;
        }
        renderDebugDocumentPage(g, document, definition, areaX, areaY, areaWidth, areaHeight);
    }

    private void renderDebugDocumentPage(GuiGraphics g, ItemStack document, CodexDocumentDefinition definition, int areaX, int areaY, int areaWidth, int areaHeight) {
        int[] fitted = fitRect(1279, 1920, areaWidth, areaHeight);
        int pageW = fitted[0];
        int pageH = fitted[1];
        int pageX = areaX + (areaWidth - pageW) / 2;
        int pageY = areaY + (areaHeight - pageH) / 2;
        g.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
        g.fill(pageX, pageY, pageX + pageW, pageY + pageH, DEBUG_PAGE);
        g.fill(pageX + pageW / 22, pageY + pageH / 8, pageX + pageW - pageW / 22, pageY + pageH / 8 + 1, DEBUG_PAGE_DARK);
        float titleScale = Math.max(0.65F, pageW / 360.0F);
        drawScaledPageString(g, "SCP", pageX + pageW / 12.0F, pageY + pageH / 22.0F, DEBUG_PAGE_DARK, titleScale * 2.2F);
        drawScaledPageString(g, definition.getDisplayName(document), pageX + pageW / 12.0F, pageY + pageH / 6.2F, DEBUG_PAGE_DARK, titleScale * 0.72F);
        int lineX = pageX + pageW / 12;
        int lineW = pageW - pageW / 6;
        int lineY = pageY + pageH / 4;
        int lineStep = Math.max(5, pageH / 42);
        for (int i = 0; i < 19; i++) g.fill(lineX, lineY + i * lineStep, lineX + lineW - ((i % 4) * pageW / 18), lineY + i * lineStep + 1, DEBUG_PAGE_FAINT);
        g.fill(lineX, lineY + lineStep * 7 - 1, lineX + lineW - pageW / 10, lineY + lineStep * 8 + 1, DEBUG_HIGHLIGHT);
        g.disableScissor();
    }

    private void drawDetailButtons(GuiGraphics g, int mouseX, int mouseY) {
        int buttonY = getButtonY();
        int gap = 6;
        int buttonX = getDetailLeft();
        int buttonWidth = (getDetailRight() - buttonX - gap) / 2;
        drawButton(g, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, "Show Document as Text", clickedTextButton(mouseX, mouseY));
        drawButton(g, buttonX + buttonWidth + gap, buttonY, buttonWidth, BUTTON_HEIGHT, "Expand Document", clickedExpandButton(mouseX, mouseY));
    }

    private void drawButton(GuiGraphics g, int x, int y, int width, int height, String label, boolean hovered) {
        g.fill(x, y, x + width, y + height, hovered ? BUTTON_BACKGROUND_HOVERED : BUTTON_BACKGROUND);
        g.drawString(mc.font, label, x + (width - mc.font.width(label)) / 2, y + 3, TEXT_WHITE, false);
    }

    private List<DisplayRow> buildRows() {
        Map<String, List<DisplayRow>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < documents.size(); i++) {
            ItemStack document = documents.get(i);
            if (document == null || document.isEmpty()) continue;
            CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(document);
            grouped.computeIfAbsent(definition.getCategory(), ignored -> new ArrayList<>()).add(DisplayRow.document(definition.getCategory(), i, definition.getDisplayName(document)));
        }
        List<DisplayRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<DisplayRow>> entry : grouped.entrySet()) {
            List<DisplayRow> docs = entry.getValue();
            docs.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));
            rows.add(DisplayRow.category(entry.getKey()));
            if (!collapsedCategories.contains(entry.getKey())) rows.addAll(docs);
        }
        return rows;
    }

    private List<String> wrapText(String body, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : body.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            String current = "";
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = current.isBlank() ? word : current + " " + word;
                if (mc.font.width(candidate) <= width || current.isBlank()) current = candidate;
                else {
                    lines.add(current);
                    current = word;
                }
            }
            if (!current.isBlank()) lines.add(current);
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private DisplayRow getClickedRow(double mouseX, double mouseY) {
        if (!isMouseOverList(mouseX, mouseY)) return null;
        int row = (int) ((mouseY - getListContentY()) / ROW_HEIGHT);
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
        if (selectedIndex >= documents.size() || (selectedIndex >= 0 && documents.get(selectedIndex).isEmpty())) {
            selectedIndex = -1;
            showingText = false;
            expandedImage = false;
            textScrollOffset = 0;
        }
    }

    private int getVisibleRows() { return Math.max(1, listHeight / ROW_HEIGHT); }
    private int getListContentY() { return y + LIST_CONTENT_Y_OFFSET; }
    private void clampScroll(int totalRows) { scrollOffset = Math.max(0, Math.min(Math.max(0, totalRows - getVisibleRows()), scrollOffset)); }
    private void clampTextScroll() { textScrollOffset = Math.max(0, textScrollOffset); }
    private void clampTextScroll(int totalLines, int visibleLines) { textScrollOffset = Math.max(0, Math.min(Math.max(0, totalLines - visibleLines), textScrollOffset)); }

    private boolean isMouseOverList(double mouseX, double mouseY) { return mouseX >= x - 1 && mouseX <= x + listWidth - 16 && mouseY >= getListContentY() && mouseY <= getListContentY() + listHeight; }
    private boolean isMouseOverDetail(double mouseX, double mouseY) { return mouseX >= getDetailLeft() && mouseX <= getDetailRight() && mouseY >= getDetailTop() && mouseY <= getDetailBottom(); }
    private boolean isInsideCodexPanels(double mouseX, double mouseY) { return isMouseOverList(mouseX, mouseY) || isMouseOverDetail(mouseX, mouseY); }
    private boolean clickedListScrollbar(double mouseX, double mouseY) { return isMouseInside(mouseX, mouseY, getListScrollbarX() - 3, getListContentY(), SCROLL_WIDTH + 6, getListScrollbarHeight()) && buildRows().size() > getVisibleRows(); }

    private void updateListScrollFromMouse(double mouseY) {
        int totalRows = buildRows().size();
        if (totalRows <= getVisibleRows()) { scrollOffset = 0; return; }
        int trackHeight = getListScrollbarHeight();
        int thumbHeight = Math.max(18, trackHeight * getVisibleRows() / totalRows);
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int maxScroll = Math.max(1, totalRows - getVisibleRows());
        scrollOffset = (int) Math.round(((mouseY - getListContentY() - (thumbHeight / 2.0D)) / thumbTravel) * maxScroll);
        clampScroll(totalRows);
    }

    private void renderScrollbar(GuiGraphics g, int totalRows) {
        if (totalRows <= getVisibleRows()) return;
        int trackX = getListScrollbarX();
        int trackY = getListContentY();
        int trackHeight = getListScrollbarHeight();
        int thumbHeight = Math.max(18, trackHeight * getVisibleRows() / totalRows);
        int maxScroll = Math.max(1, totalRows - getVisibleRows());
        int thumbY = trackY + ((trackHeight - thumbHeight) * scrollOffset / maxScroll);
        g.fill(trackX, trackY, trackX + SCROLL_WIDTH, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + SCROLL_WIDTH, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private void renderTextScrollbar(GuiGraphics g, int totalLines, int visibleLines, int trackX, int trackY, int trackHeight) {
        if (totalLines <= visibleLines) return;
        int thumbHeight = Math.max(18, trackHeight * visibleLines / totalLines);
        int maxScroll = Math.max(1, totalLines - visibleLines);
        int thumbY = trackY + ((trackHeight - thumbHeight) * textScrollOffset / maxScroll);
        g.fill(trackX, trackY, trackX + SCROLL_WIDTH, trackY + trackHeight, SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + SCROLL_WIDTH, thumbY + thumbHeight, SCROLL_THUMB);
    }

    private int getListScrollbarX() { return x + listWidth - 14; }
    private int getListScrollbarHeight() { return getVisibleRows() * ROW_HEIGHT; }
    private Optional<String> readText(CodexDocumentDefinition definition) {
        ResourceLocation textLocation = definition.getTextLocation().orElse(null);
        if (textLocation == null || mc == null) return Optional.empty();
        return mc.getResourceManager().getResource(textLocation).flatMap(resource -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) { if (builder.length() > 0) builder.append('\n'); builder.append(line); }
                return Optional.of(builder.toString());
            } catch (IOException ignored) { return Optional.empty(); }
        });
    }

    private String buildFallbackText(ItemStack document, CodexDocumentDefinition definition) { return definition.getDisplayName(document) + "\n\nNo transcription file configured for this document."; }
    private int getButtonY() { return getDetailBottom() - BUTTON_HEIGHT; }
    private int getEffectiveDetailHeight() { return Math.max(80, detailHeight); }
    private int getDetailLeft() { return detailX + DETAIL_PAD_X; }
    private int getDetailRight() { return detailX + detailWidth - DETAIL_PAD_X; }
    private int getDetailTop() { return detailY; }
    private int getDetailBottom() { return detailY + getEffectiveDetailHeight() - DETAIL_PAD_BOTTOM; }
    private int getControlY() { return getDetailTop() - 14; }
    private boolean clickedTextButton(double mouseX, double mouseY) { int gap = 6; int buttonX = getDetailLeft(); int buttonWidth = (getDetailRight() - buttonX - gap) / 2; return isMouseInside(mouseX, mouseY, buttonX, getButtonY(), buttonWidth, BUTTON_HEIGHT); }
    private boolean clickedExpandButton(double mouseX, double mouseY) { int gap = 6; int buttonX = getDetailLeft(); int buttonWidth = (getDetailRight() - buttonX - gap) / 2; return isMouseInside(mouseX, mouseY, buttonX + buttonWidth + gap, getButtonY(), buttonWidth, BUTTON_HEIGHT); }
    private boolean clickedReturnButton(double mouseX, double mouseY) { return isMouseInside(mouseX, mouseY, getDetailLeft(), getControlY(), 58, BUTTON_HEIGHT); }
    private boolean isMouseInside(double mouseX, double mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; }
    private int[] fitRect(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) { float scale = Math.min(maxWidth / (float) sourceWidth, maxHeight / (float) sourceHeight); return new int[]{Math.max(1, Math.round(sourceWidth * scale)), Math.max(1, Math.round(sourceHeight * scale))}; }
    private void setTextureFiltering(ResourceLocation texture, boolean blur) { if (mc != null) mc.getTextureManager().getTexture(texture).setFilter(blur, false); }
    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) { String prefix = "://CODEX_"; g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false); g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false); }
    private void drawScaledPageString(GuiGraphics g, String text, float x, float y, int color, float scale) { g.pose().pushPose(); g.pose().translate(x, y, 0.0F); g.pose().scale(scale, scale, 1.0F); g.drawString(mc.font, text, 0, 0, color, false); g.pose().popPose(); }
    private static int guessPanelHeight(int panelY) { Minecraft minecraft = Minecraft.getInstance(); if (minecraft == null || minecraft.getWindow() == null) return ROW_HEIGHT * 12; return Math.max(ROW_HEIGHT * 9, minecraft.getWindow().getGuiScaledHeight() - panelY - 160); }

    private static class DisplayRow {
        private final boolean categoryRow;
        private final String category;
        private final int documentIndex;
        private final String name;
        private DisplayRow(boolean categoryRow, String category, int documentIndex, String name) { this.categoryRow = categoryRow; this.category = category; this.documentIndex = documentIndex; this.name = name; }
        private static DisplayRow category(String category) { return new DisplayRow(true, category, -1, category); }
        private static DisplayRow document(String category, int documentIndex, String name) { return new DisplayRow(false, category, documentIndex, name); }
    }
}
