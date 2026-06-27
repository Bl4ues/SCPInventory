package com.bl4ues.scpinventory.item;

import java.util.Locale;
import java.util.Optional;

public enum ScpItemType {
    MISCELLANEOUS("Miscellaneous"),
    CONSUMABLE("Consumable"),
    KEY("Key"),
    CODEX("Document"),
    HEAD("Head"),
    BODY("Body"),
    LEGS("Legs"),
    FEET("Feet");

    private final String displayName;

    ScpItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEquipment() {
        return this == HEAD || this == BODY || this == LEGS || this == FEET;
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
            case "BODY", "CHEST", "CHESTPLATE", "TORSO" -> Optional.of(BODY);
            case "LEGS", "LEGGINGS" -> Optional.of(LEGS);
            case "FEET", "BOOTS" -> Optional.of(FEET);
            default -> Optional.empty();
        };
    }
}
