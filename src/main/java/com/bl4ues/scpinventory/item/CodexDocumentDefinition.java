package com.bl4ues.scpinventory.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CodexDocumentDefinition {

    private final ResourceLocation itemId;
    private final String displayName;
    private final String creator;
    private final String timestamp;
    private final String uuid;

    private CodexDocumentDefinition(ResourceLocation itemId, String displayName, String creator, String timestamp, String uuid) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.creator = creator;
        this.timestamp = timestamp;
        this.uuid = uuid;
    }

    public static Optional<CodexDocumentDefinition> parse(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return Optional.empty();
        }

        String raw = rawRule.trim();

        if (raw.contains("|")) {
            return parsePipeFormat(raw);
        }

        return parseKeyValueFormat(raw);
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

        String displayName = parts.length > 1 ? parts[1].trim() : "";
        String creator = parts.length > 2 ? parts[2].trim() : "";
        String timestamp = parts.length > 3 ? parts[3].trim() : "";
        String uuid = parts.length > 4 ? parts[4].trim() : "";

        return Optional.of(new CodexDocumentDefinition(itemId, displayName, creator, timestamp, uuid));
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
                values.getOrDefault("name", ""),
                values.getOrDefault("creator", ""),
                values.getOrDefault("timestamp", ""),
                values.getOrDefault("uuid", "")
        ));
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!itemId.equals(stackId)) {
            return false;
        }

        CompoundTag tag = stack.getTag();
        return matchesTagValue(tag, "creator", creator)
                && matchesTagValue(tag, "timestamp", timestamp)
                && matchesTagValue(tag, "uuid", uuid);
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

    public String getStableId(ItemStack fallbackStack) {
        if (!displayName.isBlank()) {
            return displayName;
        }

        if (fallbackStack != null && !fallbackStack.isEmpty() && fallbackStack.hasTag()) {
            return itemId + "|" + fallbackStack.getTag();
        }

        return itemId + "|" + creator + "|" + timestamp + "|" + uuid;
    }

    private static boolean matchesTagValue(CompoundTag tag, String key, String expected) {
        if (expected == null || expected.isBlank()) {
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

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.replace(" ", "")
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();
    }
}
