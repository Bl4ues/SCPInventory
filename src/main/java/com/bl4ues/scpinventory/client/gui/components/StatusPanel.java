package com.bl4ues.scpinventory.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatusPanel {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int ROW_BACKGROUND = 0x25303638;
    private static final int LINE_GRAY = 0x446A6C6C;
    private static final int BAR_BACKGROUND = 0x55303638;
    private static final int BAR_GOOD = 0xAA6FA07A;
    private static final int BAR_WARN = 0xAAA09A6F;
    private static final int BAR_BAD = 0xAAA06F6F;

    private static final int CONDITION_ROW_HEIGHT = 30;
    private static final int CONDITION_ICON_SIZE = 20;

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
        renderParameters(g);
    }

    private void renderConditions(GuiGraphics g) {
        if (mc.player == null) return;

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(Comparator.comparing(effect -> effect.getEffect().getDisplayName().getString()));

        int contentX = conditionsX + 16;
        int contentY = conditionsY + 18;
        int contentWidth = conditionsWidth - 32;
        int visibleRows = Math.max(1, (conditionsHeight - 36) / CONDITION_ROW_HEIGHT);

        if (effects.isEmpty()) {
            g.drawString(mc.font, "No active conditions.", contentX, contentY, TEXT_GRAY, false);
            return;
        }

        for (int i = 0; i < effects.size() && i < visibleRows; i++) {
            MobEffectInstance effect = effects.get(i);
            int rowY = contentY + (i * CONDITION_ROW_HEIGHT);
            renderConditionRow(g, effect, contentX, rowY, contentWidth);
        }
    }

    private void renderConditionRow(GuiGraphics g, MobEffectInstance effect, int x, int y, int width) {
        g.fill(x, y, x + width, y + CONDITION_ROW_HEIGHT - 3, ROW_BACKGROUND);

        int iconX = x + 6;
        int iconY = y + 4;
        g.fill(iconX, iconY, iconX + CONDITION_ICON_SIZE, iconY + CONDITION_ICON_SIZE, BAR_BACKGROUND);
        g.fill(iconX, iconY, iconX + CONDITION_ICON_SIZE, iconY + 1, LINE_GRAY);
        g.fill(iconX, iconY + CONDITION_ICON_SIZE - 1, iconX + CONDITION_ICON_SIZE, iconY + CONDITION_ICON_SIZE, LINE_GRAY);
        g.fill(iconX, iconY, iconX + 1, iconY + CONDITION_ICON_SIZE, LINE_GRAY);
        g.fill(iconX + CONDITION_ICON_SIZE - 1, iconY, iconX + CONDITION_ICON_SIZE, iconY + CONDITION_ICON_SIZE, LINE_GRAY);

        String name = effect.getEffect().getDisplayName().getString() + getAmplifierSuffix(effect);
        String duration = formatDuration(effect.getDuration());
        int textX = iconX + CONDITION_ICON_SIZE + 8;
        g.drawString(mc.font, name, textX, y + 5, TEXT_WHITE, false);
        g.drawString(mc.font, duration, textX, y + 17, TEXT_GRAY, false);

        int barX = textX + 92;
        int barY = y + 18;
        int barWidth = Math.max(32, x + width - barX - 8);
        g.fill(barX, barY, barX + barWidth, barY + 3, BAR_BACKGROUND);
        int fill = Math.max(2, Math.min(barWidth, Math.round(barWidth * getDurationRatio(effect))));
        g.fill(barX, barY, barX + fill, barY + 3, getConditionBarColor(effect));
    }

    private void renderParameters(GuiGraphics g) {
        if (mc.player == null) return;

        int contentX = parametersX + 24;
        int contentY = parametersY + 26;
        int avatarX = contentX;
        int avatarY = contentY;
        int avatarW = Math.min(108, parametersWidth / 3);
        int avatarH = Math.min(190, parametersHeight - 70);

        g.fill(avatarX, avatarY, avatarX + avatarW, avatarY + avatarH, 0xAA101010);
        g.drawString(mc.font, "Player Preview", avatarX + 14, avatarY + (avatarH / 2) - 4, TEXT_GRAY, false);

        int parameterX = avatarX + avatarW + 28;
        int rowY = contentY + 8;
        drawParameterLine(g, parameterX, rowY, "Max Health", Math.round(mc.player.getMaxHealth()));
        drawParameterLine(g, parameterX, rowY + 34, "Armor", mc.player.getArmorValue());
        drawParameterLine(g, parameterX, rowY + 68, "Stamina", 100);
        drawParameterLine(g, parameterX, rowY + 102, "Air", mc.player.getAirSupply());
    }

    private void drawParameterLine(GuiGraphics g, int x, int y, String name, int value) {
        g.drawString(mc.font, name, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, Integer.toString(value), x, y + 12, TEXT_WHITE, false);
        g.fill(x, y + 25, x + 150, y + 26, LINE_GRAY);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://STATUS_";
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, suffix, x + mc.font.width(prefix), y, TEXT_WHITE, false);
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
        return Math.max(0.05F, Math.min(1.0F, ticks / 1200.0F));
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
