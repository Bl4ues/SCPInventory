package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.Optional;

public final class ScpItemClassifier {

    private ScpItemClassifier() {
    }

    public static ScpItemType getType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ScpItemType.MISCELLANEOUS;
        }

        Optional<CodexDocumentDefinition> codexDocument = getCodexDocument(stack);
        if (codexDocument.isPresent()) {
            return ScpItemType.CODEX;
        }

        Optional<ScpItemType> configuredType = getConfiguredType(stack);
        if (configuredType.isPresent()) {
            return configuredType.get();
        }

        if (isDefaultConsumable(stack)) {
            return ScpItemType.CONSUMABLE;
        }

        if (stack.getItem() instanceof ArmorItem armorItem) {
            return fromVanillaEquipmentSlot(armorItem.getEquipmentSlot());
        }

        return ScpItemType.MISCELLANEOUS;
    }

    public static Optional<ScpEquipmentSlot> getEquipmentSlot(ItemStack stack) {
        return getType(stack).getEquipmentSlot();
    }

    public static String getDisplayType(ItemStack stack) {
        return getType(stack).getDisplayName();
    }

    public static String getCodexDisplayName(ItemStack stack) {
        return getCodexDocument(stack)
                .map(document -> document.getDisplayName(stack))
                .orElseGet(() -> stack == null || stack.isEmpty() ? "Unknown Document" : stack.getHoverName().getString());
    }

    public static CodexDocumentDefinition getCodexDefinitionOrFallback(ItemStack stack) {
        return getCodexDocument(stack).orElseGet(() -> CodexDocumentDefinition.fallback(stack));
    }

    public static Optional<CodexDocumentDefinition> getCodexDocument(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        for (String rawRule : ScpInventoryConfig.CODEX_DOCUMENTS.get()) {
            Optional<CodexDocumentDefinition> definition = CodexDocumentDefinition.parse(rawRule);
            if (definition.isPresent() && definition.get().matches(stack)) {
                return definition;
            }
        }

        return Optional.empty();
    }

    private static boolean isDefaultConsumable(ItemStack stack) {
        if (stack.isEdible()) {
            return true;
        }

        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.EAT || animation == UseAnim.DRINK) {
            return true;
        }

        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stackId == null) {
            return false;
        }

        String path = stackId.getPath();
        return path.equals("potion")
                || path.equals("splash_potion")
                || path.equals("lingering_potion")
                || path.endsWith("_potion")
                || path.contains("potion");
    }

    private static Optional<ScpItemType> getConfiguredType(ItemStack stack) {
        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        for (String rawRule : ScpInventoryConfig.ITEM_RULES.get()) {
            if (rawRule == null || rawRule.isBlank()) {
                continue;
            }

            String[] parts = rawRule.split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }

            ResourceLocation configuredId = ResourceLocation.tryParse(parts[0].trim());
            if (configuredId == null || !configuredId.equals(stackId)) {
                continue;
            }

            Optional<ScpItemType> type = ScpItemType.fromConfigToken(parts[1]);
            if (type.isPresent()) {
                return type;
            }
        }

        return Optional.empty();
    }

    private static ScpItemType fromVanillaEquipmentSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ScpItemType.HEAD;
            case CHEST -> ScpItemType.CHEST;
            case LEGS -> ScpItemType.LEGS;
            case FEET -> ScpItemType.FEET;
            default -> ScpItemType.MISCELLANEOUS;
        };
    }
}
