package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CodexDocumentDefinition {

    public static final String DEBUG_TAG = "ScpCodexDebug";

    private static final ResourceLocation DEBUG_TEXT = new ResourceLocation(ScpInventoryMod.MODID, "codex/debug_paper.txt");
    private static final int DEFAULT_IMAGE_WIDTH = 1279;
    private static final int DEFAULT_IMAGE_HEIGHT = 1920;

    private final ResourceLocation itemId;
    private final String category;
    private final String displayName;
    private final ResourceLocation imageLocation;
    private final ResourceLocation textLocation;
    private final int imageWidth;
    private final int imageHeight;
    private final String creator;
    private final String timestamp;
    private final String uuid;
    private final String nbtKey;
    private final String nbtValue;

    private CodexDocumentDefinition(
            ResourceLocation itemId,
            String category,
            String displayName,
            ResourceLocation imageLocation,
            ResourceLocation textLocation,
            int imageWidth,
            int imageHeight,
            String creator,
            String timestamp,
            String uuid,
            String nbtKey,
            String nbtValue
    ) {
        this.itemId = itemId;
        this.category = cleanOrDefault(category, "Documents");
        this.displayName = displayName == null ? "" : displayName.trim();
        this.imageLocation = imageLocation;
        this.textLocation = textLocation;
        this.imageWidth = Math.max(1, imageWidth);
        this.imageHeight = Math.max(1, imageHeight);
        this.creator = creator == null ? "" : creator.trim();
        this.timestamp = timestamp == null ? "" : timestamp.trim();
        this.uuid = uuid == null ? "" : uuid.trim();
        this.nbtKey = nbtKey == null ? "" : nbtKey.trim();
        this.nbtValue = nbtValue == null ? "" : nbtValue.trim();
    }

    public static Optional<CodexDocumentDefinition> parse(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return Optional.empty();
        }

        String raw = rawRule.trim();

        if (raw.contains("=") && raw.contains(";")) {
            return parseKeyValueFormat(raw);
        }

        if (raw.contains("|")) {
            return parsePipeFormat(raw);
        }

        ResourceLocation itemId = ResourceLocation.tryParse(raw);
        if (itemId == null) {
            return Optional.empty();
        }

        return Optional.of(new CodexDocumentDefinition(
                itemId,
                "Documents",
                "",
                null,
                null,
                DEFAULT_IMAGE_WIDTH,
                DEFAULT_IMAGE_HEIGHT,
                "",
                "",
                "",
                "",
                ""
        ));
    }

    public static Optional<CodexDocumentDefinition> getBuiltIn(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.PAPER || !stack.hasTag()) {
            return Optional.empty();
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(DEBUG_TAG)) {
            return Optional.empty();
        }

        return Optional.of(new CodexDocumentDefinition(
                BuiltInRegistries.ITEM.getKey(Items.PAPER),
                "SCP Documents",
                "Debug Paper Document",
                null,
                DEBUG_TEXT,
                DEFAULT_IMAGE_WIDTH,
                DEFAULT_IMAGE_HEIGHT,
                "",
                "",
                "",
                DEBUG_TAG,
                "true"
        ));
    }

    public static CodexDocumentDefinition fallback(ItemStack stack) {
        ResourceLocation itemId = stack == null || stack.isEmpty()
                ? new ResourceLocation("minecraft", "air")
                : BuiltInRegistries.ITEM.getKey(stack.getItem());

        String name = stack == null || stack.isEmpty()
                ? "Unknown Document"
                : stack.getHoverName().getString();

        return new CodexDocumentDefinition(
                itemId,
                "Documents",
                name,
                null,
                null,
                DEFAULT_IMAGE_WIDTH,
                DEFAULT_IMAGE_HEIGHT,
                "",
                "",
                "",
                "",
                ""
        );
    }

    private static Optional<CodexDocumentDefinition> parsePipeFormat(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length == 0 || parts[0].isBlank()) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0].trim());
        if (itemId == null) {
            return Optional.empty();
        }

        if (parts.length >= 6) {
            return Optional.of(new CodexDocumentDefinition(
                    itemId,
                    getPart(parts, 1),
                    getPart(parts, 2),
                    parseLocation(getPart(parts, 3)),
                    parseLocation(getPart(parts, 4)),
                    parseInt(getPart(parts, 8), DEFAULT_IMAGE_WIDTH),
                    parseInt(getPart(parts, 9), DEFAULT_IMAGE_HEIGHT),
                    getPart(parts, 5),
                    getPart(parts, 6),
                    getPart(parts, 7),
                    "",
                    ""
            ));
        }

        return Optional.of(new CodexDocumentDefinition(
                itemId,
                "Documents",
                getPart(parts, 1),
                null,
                null,
                DEFAULT_IMAGE_WIDTH,
                DEFAULT_IMAGE_HEIGHT,
                getPart(parts, 2),
                getPart(parts, 3),
                getPart(parts, 4),
                "",
                ""
        ));
    }

    private static Optional<CodexDocumentDefinition> parseKeyValueFormat(String raw) {
        Map<String, String> values = new HashMap<>();

        int uuidStart = raw.indexOf("uuid=");
        String beforeUuid = uuidStart >= 0 ? raw.substring(0, uuidStart) : raw;
        if (uuidStart >= 0) {
            values.put("uuid", raw.substring(uuidStart + "uuid=".length()).trim());
        }

        for (String part : beforeUuid.split(";")) {
            if (part.isBlank()) {
                continue;
            }

            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                values.put(pair[0].trim().toLowerCase(), pair[1].trim());
            }
        }

        String id = values.get("id");
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(id);
        if (itemId == null) {
            return Optional.empty();
        }

        return Optional.of(new CodexDocumentDefinition(
                itemId,
                firstPresent(values, "category", "type", "section"),
                firstPresent(values, "name", "display_name", "title"),
                parseLocation(firstPresent(values, "image", "texture", "photo")),
                parseLocation(firstPresent(values, "text", "transcript", "transcription")),
                parseInt(firstPresent(values, "image_width", "width"), DEFAULT_IMAGE_WIDTH),
                parseInt(firstPresent(values, "image_height", "height"), DEFAULT_IMAGE_HEIGHT),
                values.getOrDefault("creator", ""),
                values.getOrDefault("timestamp", ""),
                values.getOrDefault("uuid", ""),
                firstPresent(values, "nbt_key", "tag_key"),
                firstPresent(values, "nbt_value", "tag_value")
        ));
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!itemId.equals(stackId)) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        return matchesTagValue(tag, "creator", creator)
                && matchesTagValue(tag, "timestamp", timestamp)
                && matchesTagValue(tag, "uuid", uuid)
                && matchesTagValue(tag, nbtKey, nbtValue);
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayName(ItemStack fallbackStack) {
        if (!displayName.isBlank()) {
            return displayName;
        }

        if (fallbackStack != null && !fallbackStack.isEmpty()) {
            return fallbackStack.getHoverName().getString();
        }

        return itemId.toString();
    }

    public Optional<ResourceLocation> getImageLocation() {
        return Optional.ofNullable(imageLocation);
    }

    public Optional<ResourceLocation> getTextLocation() {
        return Optional.ofNullable(textLocation);
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public String getStableId(ItemStack fallbackStack) {
        return category + "|" + getDisplayName(fallbackStack) + "|" + itemId + "|" + creator + "|" + timestamp + "|" + uuid;
    }

    private static boolean matchesTagValue(CompoundTag tag, String key, String expected) {
        if (key == null || key.isBlank() || expected == null || expected.isBlank()) {
            return true;
        }

        if (tag == null || !tag.contains(key)) {
            return false;
        }

        Tag actual = tag.get(key);
        if (actual == null) {
            return false;
        }

        return normalize(actual.getAsString()).equals(normalize(expected))
                || normalize(actual.toString()).equals(normalize(expected));
    }

    private static ResourceLocation parseLocation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return ResourceLocation.tryParse(value.trim());
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String firstPresent(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private static String getPart(String[] parts, int index) {
        return index >= 0 && index < parts.length ? parts[index].trim() : "";
    }

    private static String cleanOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.replace(" ", "")
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("b", "")
                .trim()
                .toLowerCase();
    }
}
