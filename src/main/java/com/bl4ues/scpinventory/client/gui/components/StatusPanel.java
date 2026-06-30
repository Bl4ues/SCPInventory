package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.client.PlayerVitalsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StatusPanel {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int ROW_BACKGROUND = 0x24303638;
    private static final int PREVIEW_BACKGROUND = 0x70303638;
    private static final int PREVIEW_BACKGROUND_INNER = 0x66181E20;
    private static final int LINE_GRAY = 0x446A6C6C;
    private static final int LINE_LIGHT = 0x776A6C6C;
    private static final int BAR_BACKGROUND = 0x55303638;
    private static final int BAR_GOOD = 0xAA6FA07A;
    private static final int BAR_WARN = 0xAAA09A6F;
    private static final int BAR_BAD = 0xAAA06F6F;
    private static final int SCROLL_TRACK = 0x55303638;
    private static final int SCROLL_THUMB = 0xAA6A6C6C;

    private static final int CONDITION_ROW_HEIGHT = 38;
    private static final int CONDITION_ICON_SIZE = 24;
    private static final int CONDITIONS_PAD_X = 28;
    private static final int CONDITIONS_PAD_TOP = 10;
    private static final int PARAMETERS_PAD_X = 34;
    private static final int PARAMETERS_PAD_TOP = 34;

    private final Minecraft mc = Minecraft.getInstance();
    private final int conditionsX;
    private final int conditionsY;
    private final int conditionsWidth;
    private final int conditionsHeight;
    private final int parametersX;
    private final int parametersY;
    private final int parametersWidth;
    private final int parametersHeight;
    private final int titleY;
    private final int conditionsTitleX;
    private final int parametersTitleX;

    private int conditionsScroll = 0;
    private boolean conditionsScrollbarDragging = false;

    public StatusPanel(int conditionsX, int conditionsY, int conditionsWidth, int conditionsHeight,
                       int parametersX, int parametersY, int parametersWidth, int parametersHeight,
                       int titleY, int conditionsTitleX, int parametersTitleX) {
        this.conditionsX = conditionsX;
        this.conditionsY = conditionsY;
        this.conditionsWidth = conditionsWidth;
        this.conditionsHeight = conditionsHeight;
        this.parametersX = parametersX;
        this.parametersY = parametersY;
        this.parametersWidth = parametersWidth;
        this.parametersHeight = parametersHeight;
        this.titleY = titleY;
        this.conditionsTitleX = conditionsTitleX;
        this.parametersTitleX = parametersTitleX;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitle(g, conditionsTitleX, titleY, "CONDITIONS");
        drawSectionTitle(g, parametersTitleX, titleY, "PARAMETERS");
        renderConditions(g);
        renderParameters(g, mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOverConditions(mouseX, mouseY) || mc.player == null) return false;
        int totalRows = mc.player.getActiveEffects().size();
        int visibleRows = getVisibleConditionRows();
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (maxScroll <= 0) return false;
        conditionsScroll = Math.max(0, Math.min(maxScroll, conditionsScroll - (int) Math.signum(delta)));
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button != 0 || !hasScrollableConditions() || !isMouseOverConditionScrollbar(mouseX, mouseY)) return false;
        conditionsScrollbarDragging = true;
        updateConditionsScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseDraggedScrollbar(double mouseY) {
        if (!conditionsScrollbarDragging) return false;
        updateConditionsScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleasedScrollbar(int button) {
        if (button != 0 || !conditionsScrollbarDragging) return false;
        conditionsScrollbarDragging = false;
        return true;
    }

    private void renderConditions(GuiGraphics g) {
        if (mc.player == null) return;

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(Comparator.comparing(effect -> effect.getEffect().getDisplayName().getString()));

        if (effects.isEmpty()) return;

        int contentX = conditionsX + CONDITIONS_PAD_X;
        int contentY = conditionsY + CONDITIONS_PAD_TOP;
        int contentWidth = conditionsWidth - (CONDITIONS_PAD_X * 2) - 14;
        int visibleRows = getVisibleConditionRows();
        int maxScroll = Math.max(0, effects.size() - visibleRows);
        conditionsScroll = Math.max(0, Math.min(maxScroll, conditionsScroll));

        for (int i = 0; i < visibleRows; i++) {
            int effectIndex = i + conditionsScroll;
            if (effectIndex >= effects.size()) break;
            MobEffectInstance effect = effects.get(effectIndex);
            int rowY = contentY + (i * CONDITION_ROW_HEIGHT);
            renderConditionRow(g, effect, contentX, rowY, contentWidth);
        }

        if (effects.size() > visibleRows) {
            renderConditionScrollbar(g, effects.size(), visibleRows);
        }
    }

    private int getVisibleConditionRows() {
        return Math.max(1, (conditionsHeight - CONDITIONS_PAD_TOP - 12) / CONDITION_ROW_HEIGHT);
    }

    private void renderConditionScrollbar(GuiGraphics g, int totalRows, int visibleRows) {
        int trackX = getConditionScrollbarX();
        int trackY = getConditionScrollbarY();
        int trackH = getConditionScrollbarHeight();
        g.fill(trackX, trackY, trackX + 4, trackY + trackH, SCROLL_TRACK);

        int thumbH = getConditionScrollbarThumbHeight(totalRows, visibleRows);
        int thumbY = getConditionScrollbarThumbY(totalRows, visibleRows, thumbH);
        g.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, SCROLL_THUMB);
    }

    private void renderConditionRow(GuiGraphics g, MobEffectInstance effect, int x, int y, int width) {
        g.fill(x, y, x + width, y + CONDITION_ROW_HEIGHT - 5, ROW_BACKGROUND);

        int iconX = x + 8;
        int iconY = y + 5;
        drawIconFrame(g, iconX, iconY);
        renderEffectIcon(g, effect, iconX + 3, iconY + 3);

        String name = effect.getEffect().getDisplayName().getString() + getAmplifierSuffix(effect);
        String duration = formatDuration(effect.getDuration());
        int textX = iconX + CONDITION_ICON_SIZE + 10;
        g.drawString(mc.font, name, textX, y + 7, TEXT_WHITE, false);
        g.drawString(mc.font, duration, textX, y + 20, TEXT_GRAY, false);

        int barWidth = Math.max(90, Math.min(150, width / 3));
        int barX = x + width - barWidth - 14;
        int barY = y + 19;
        g.fill(barX, barY, barX + barWidth, barY + 3, BAR_BACKGROUND);
        int fill = Math.max(2, Math.min(barWidth, Math.round(barWidth * getDurationRatio(effect))));
        g.fill(barX, barY, barX + fill, barY + 3, getConditionBarColor(effect));
    }

    private void drawIconFrame(GuiGraphics g, int x, int y) {
        int right = x + CONDITION_ICON_SIZE;
        int bottom = y + CONDITION_ICON_SIZE;
        g.fill(x, y, right, bottom, 0x30303638);
        g.fill(x, y, right, y + 1, LINE_GRAY);
        g.fill(x, bottom - 1, right, bottom, LINE_GRAY);
        g.fill(x, y, x + 1, bottom, LINE_GRAY);
        g.fill(right - 1, y, right, bottom, LINE_GRAY);
    }

    private void renderEffectIcon(GuiGraphics g, MobEffectInstance effect, int x, int y) {
        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());
        if (sprite == null) return;
        g.blit(x, y, 0, 18, 18, sprite);
    }

    private void renderParameters(GuiGraphics g, int mouseX, int mouseY) {
        if (mc.player == null) return;

        int contentX = parametersX + PARAMETERS_PAD_X;
        int contentY = parametersY + PARAMETERS_PAD_TOP;
        int contentRight = parametersX + parametersWidth - PARAMETERS_PAD_X;

        int avatarW = Math.min(118, parametersWidth / 3);
        int avatarH = Math.min(210, parametersHeight - 82);
        int parameterW = Math.min(150, Math.max(118, parametersWidth / 3));
        int groupGap = 40;
        int groupW = avatarW + groupGap + parameterW;
        int groupX = contentX + Math.max(0, ((contentRight - contentX) - groupW) / 2);
        int groupY = contentY + Math.max(0, (parametersHeight - PARAMETERS_PAD_TOP - avatarH) / 2 - 22);

        int previewLeft = groupX;
        int previewTop = groupY;
        int previewRight = previewLeft + avatarW;
        int previewBottom = previewTop + avatarH;
        drawPreviewBox(g, previewLeft, previewTop, previewRight, previewBottom);

        int previewCenterX = previewLeft + (avatarW / 2);
        int previewBottomY = previewBottom - 38;
        int previewScale = Math.max(34, Math.min(56, avatarH / 4));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewCenterX, previewBottomY, previewScale,
                previewCenterX - mouseX, previewTop + 52 - mouseY, mc.player);

        int parameterX = previewRight + groupGap;
        int parameterRight = Math.min(contentRight, parameterX + parameterW);
        int rowY = previewTop + 20;
        int rowGap = 33;

        drawParameterLine(g, parameterX, rowY, parameterRight, "Max Health", formatValue(getAttributeValue(Attributes.MAX_HEALTH)));
        drawParameterLine(g, parameterX, rowY + rowGap, parameterRight, "Armor", formatValue(getAttributeValue(Attributes.ARMOR)));
        drawParameterLine(g, parameterX, rowY + (rowGap * 2), parameterRight, "Toughness", formatValue(getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
        drawParameterLine(g, parameterX, rowY + (rowGap * 3), parameterRight, "Attack", formatValue(getAttributeValue(Attributes.ATTACK_DAMAGE)));
        drawParameterLine(g, parameterX, rowY + (rowGap * 4), parameterRight, "Stamina", formatValue(PlayerVitalsClient.getStamina()) + "/" + formatValue(PlayerVitalsClient.getMaxStamina()));
    }

    private void drawPreviewBox(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left, top, right, bottom, PREVIEW_BACKGROUND);
        g.fill(left + 2, top + 2, right - 2, bottom - 2, PREVIEW_BACKGROUND_INNER);
        int corner = 12;
        g.fill(left, top, left + corner, top + 1, LINE_LIGHT);
        g.fill(left, top, left + 1, top + corner, LINE_LIGHT);
        g.fill(right - corner, top, right, top + 1, LINE_LIGHT);
        g.fill(right - 1, top, right, top + corner, LINE_LIGHT);
        g.fill(left, bottom - 1, left + corner, bottom, LINE_LIGHT);
        g.fill(left, bottom - corner, left + 1, bottom, LINE_LIGHT);
        g.fill(right - corner, bottom - 1, right, bottom, LINE_LIGHT);
        g.fill(right - 1, bottom - corner, right, bottom, LINE_LIGHT);
    }

    private void drawParameterLine(GuiGraphics g, int x, int y, int right, String name, String value) {
        g.drawString(mc.font, name, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, value, x, y + 12, TEXT_WHITE, false);
        g.fill(x, y + 25, right, y + 26, LINE_GRAY);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://STATUS_";
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false);
    }

    private boolean hasScrollableConditions() {
        return mc.player != null && mc.player.getActiveEffects().size() > getVisibleConditionRows();
    }

    private boolean isMouseOverConditions(double mouseX, double mouseY) {
        return mouseX >= conditionsX && mouseX <= conditionsX + conditionsWidth
                && mouseY >= conditionsY && mouseY <= conditionsY + conditionsHeight;
    }

    private boolean isMouseOverConditionScrollbar(double mouseX, double mouseY) {
        int trackX = getConditionScrollbarX();
        int trackY = getConditionScrollbarY();
        int trackH = getConditionScrollbarHeight();
        return mouseX >= trackX - 4 && mouseX <= trackX + 8
                && mouseY >= trackY && mouseY <= trackY + trackH;
    }

    private int getConditionScrollbarX() {
        return conditionsX + conditionsWidth - 12;
    }

    private int getConditionScrollbarY() {
        return conditionsY + 10;
    }

    private int getConditionScrollbarHeight() {
        return conditionsHeight - 20;
    }

    private int getConditionScrollbarThumbHeight(int totalRows, int visibleRows) {
        return Math.max(24, Math.round(getConditionScrollbarHeight() * (visibleRows / (float) totalRows)));
    }

    private int getConditionScrollbarThumbY(int totalRows, int visibleRows, int thumbH) {
        int maxScroll = Math.max(1, totalRows - visibleRows);
        int travel = Math.max(1, getConditionScrollbarHeight() - thumbH);
        return getConditionScrollbarY() + Math.round(travel * (conditionsScroll / (float) maxScroll));
    }

    private void updateConditionsScrollFromMouse(double mouseY) {
        if (mc.player == null) return;
        int totalRows = mc.player.getActiveEffects().size();
        int visibleRows = getVisibleConditionRows();
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (maxScroll <= 0) {
            conditionsScroll = 0;
            return;
        }

        int thumbH = getConditionScrollbarThumbHeight(totalRows, visibleRows);
        int travel = Math.max(1, getConditionScrollbarHeight() - thumbH);
        double normalized = (mouseY - getConditionScrollbarY() - (thumbH / 2.0D)) / travel;
        conditionsScroll = Math.max(0, Math.min(maxScroll, (int) Math.round(normalized * maxScroll)));
    }

    private double getAttributeValue(Attribute attribute) {
        if (mc.player == null || mc.player.getAttribute(attribute) == null) return 0.0D;
        return mc.player.getAttributeValue(attribute);
    }

    private String formatValue(double value) {
        double rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05D) return Integer.toString((int) rounded);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String formatDuration(int ticks) {
        if (ticks < 0 || ticks >= 32147) return "∞";
        int seconds = Math.max(0, ticks / 20);
        int minutes = seconds / 60;
        seconds %= 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private float getDurationRatio(MobEffectInstance effect) {
        int ticks = effect.getDuration();
        if (ticks < 0 || ticks >= 32147) return 1.0F;
        return Math.max(0.05F, Math.min(1.0F, ticks / 3600.0F));
    }

    private int getConditionBarColor(MobEffectInstance effect) {
        float ratio = getDurationRatio(effect);
        if (ratio > 0.55F) return BAR_GOOD;
        if (ratio > 0.25F) return BAR_WARN;
        return BAR_BAD;
    }

    private String getAmplifierSuffix(MobEffectInstance effect) {
        int level = effect.getAmplifier() + 1;
        if (level <= 1) return "";
        return " " + toRoman(level);
    }

    private String toRoman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(value);
        };
    }
}
