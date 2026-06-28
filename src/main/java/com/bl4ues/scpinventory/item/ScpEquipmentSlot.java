package com.bl4ues.scpinventory.item;

import java.util.Locale;
import java.util.Optional;

public enum ScpEquipmentSlot {
    HEAD("Head", "Head"),
    ACCESSORY("Accessory", "Accessory"),
    CHEST("Chest", "Chest"),
    LEGS("Legs", "Legs"),
    FEET("Feet", "Feet"),
    WEAPON("Weapon", "Weapon");

    private final String displayName;
    private final String tagName;

    ScpEquipmentSlot(String displayName, String tagName) {
        this.displayName = displayName;
        this.tagName = tagName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTagName() {
        return tagName;
    }

    public static Optional<ScpEquipmentSlot> fromName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "HEAD", "HELMET", "MASK" -> Optional.of(HEAD);
            case "ACCESSORY", "ACCESSORY_1", "TRINKET", "RING", "AMULET" -> Optional.of(ACCESSORY);
            case "BODY", "CHEST", "CHESTPLATE", "TORSO" -> Optional.of(CHEST);
            case "LEGS", "LEGGINGS", "PANTS" -> Optional.of(LEGS);
            case "FEET", "BOOTS", "SHOES" -> Optional.of(FEET);
            case "WEAPON", "MAINHAND", "MAIN_HAND", "HAND" -> Optional.of(WEAPON);
            default -> Optional.empty();
        };
    }
}
