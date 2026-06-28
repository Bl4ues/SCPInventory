package com.bl4ues.scpinventory.item;

import java.util.Locale;
import java.util.Optional;

public enum ScpItemType {
    MISCELLANEOUS("Miscellaneous"),
    CONSUMABLE("Consumable"),
    KEY("Key"),
    CODEX("Document"),
    HEAD("Head"),
    ACCESSORY("Accessory"),
    BODY("Body"),
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
            case ACCESSORY -> Optional.of(ScpEquipmentSlot.ACCESSORY);
            case BODY -> Optional.of(ScpEquipmentSlot.BODY);
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
            case "CONSUMABLE", "USE", "USABLE" -> Optional.of(CONSUMABLE);
            case "KEY", "KEYCARD", "KEYRING" -> Optional.of(KEY);
            case "CODEX", "DOCUMENT", "DOC" -> Optional.of(CODEX);
            case "HEAD", "HELMET", "MASK" -> Optional.of(HEAD);
            case "ACCESSORY", "TRINKET", "RING", "AMULET" -> Optional.of(ACCESSORY);
            case "BODY", "CHEST", "CHESTPLATE", "TORSO" -> Optional.of(BODY);
            case "WEAPON", "MAINHAND", "MAIN_HAND", "HAND" -> Optional.of(WEAPON);
            default -> Optional.empty();
        };
    }
}
