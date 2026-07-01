package com.bl4ues.scpinventory.item;

import java.util.Locale;
import java.util.Optional;

public enum ScpItemType {
    MISCELLANEOUS("Miscellaneous"),
    CONSUMABLE("Consumable"),
    USABLE("Usable"),
    KEY("Key"),
    CODEX("Document"),
    COIN("Coin"),
    HEAD("Head"),
    ACCESSORY("Accessory"),
    ACCESSORY_HAND("Accessory"),
    CHEST("Chest"),
    LEGS("Legs"),
    FEET("Feet"),
    WEAPON("Weapon");

    private final String displayName;

    ScpItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEquipment() {
        return getEquipmentSlot().isPresent();
    }

    public Optional<ScpEquipmentSlot> getEquipmentSlot() {
        return switch (this) {
            case HEAD -> Optional.of(ScpEquipmentSlot.HEAD);
            case ACCESSORY, ACCESSORY_HAND -> Optional.of(ScpEquipmentSlot.ACCESSORY);
            case CHEST -> Optional.of(ScpEquipmentSlot.CHEST);
            case LEGS -> Optional.of(ScpEquipmentSlot.LEGS);
            case FEET -> Optional.of(ScpEquipmentSlot.FEET);
            case WEAPON -> Optional.of(ScpEquipmentSlot.WEAPON);
            default -> Optional.empty();
        };
    }

    public static Optional<ScpItemType> fromConfigToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return switch (token.trim().toUpperCase(Locale.ROOT)) {
            case "MISC", "MISCELLANEOUS", "MISCELLANEOUSLY" -> Optional.of(MISCELLANEOUS);
            case "CONSUMABLE", "CONSUME", "USE" -> Optional.of(CONSUMABLE);
            case "USABLE", "USEABLE", "NON_CONSUMABLE", "NONCONSUMABLE", "RIGHT_CLICK", "RIGHTCLICK" -> Optional.of(USABLE);
            case "KEY", "KEYCARD", "KEYRING" -> Optional.of(KEY);
            case "CODEX", "DOCUMENT", "DOC" -> Optional.of(CODEX);
            case "COIN", "CURRENCY", "TOKEN", "MONEY" -> Optional.of(COIN);
            case "HEAD", "HELMET", "MASK" -> Optional.of(HEAD);
            case "ACCESSORY", "TRINKET", "RING", "AMULET" -> Optional.of(ACCESSORY);
            case "ACCESSORYHAND", "ACCESSORY_HAND", "ACCESSORYOFFHAND", "ACCESSORY_OFFHAND",
                 "OFFHAND_ACCESSORY", "OFFHANDACCESSORY", "ACCESORYHAND", "ACCESORY_HAND" -> Optional.of(ACCESSORY_HAND);
            case "BODY", "CHEST", "CHESTPLATE", "TORSO" -> Optional.of(CHEST);
            case "LEGS", "LEGGINGS", "PANTS" -> Optional.of(LEGS);
            case "FEET", "BOOTS", "SHOES" -> Optional.of(FEET);
            case "WEAPON", "MAINHAND", "MAIN_HAND", "HAND" -> Optional.of(WEAPON);
            default -> Optional.empty();
        };
    }
}
