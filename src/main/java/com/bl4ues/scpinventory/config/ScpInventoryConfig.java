package com.bl4ues.scpinventory.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ScpInventoryConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_RULES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CODEX_DOCUMENTS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("scp_inventory");

        ITEM_RULES = builder
                .comment(
                        "Item classification rules used by the SCP inventory.",
                        "Format: modid:item|TYPE",
                        "Accepted TYPE values: MISC, CONSUMABLE, KEY, CODEX, HEAD, BODY, CHEST, LEGS, FEET, ACCESSORY, WEAPON.",
                        "Items not listed here fall back to vanilla detection: food becomes Consumable, vanilla armor goes to its matching equipment slot, everything else becomes Miscellaneous.",
                        "Examples:",
                        "  item_rules = [\"scp_additions:level_2_keycard|KEY\", \"scp_additions:gas_mask|HEAD\", \"scp_additions:scp_714|ACCESSORY\", \"minecraft:golden_apple|CONSUMABLE\"]"
                )
                .defineList("item_rules", List.<String>of(), ScpInventoryConfig::isString);

        CODEX_DOCUMENTS = builder
                .comment(
                        "Codex document rules. Matching documents are unlocked into the Codex and do not consume main inventory slots.",
                        "IMPORTANT: this is a TOML string list. Each whole document rule must be wrapped in quotes.",
                        "Correct example:",
                        "  codex_documents = [\"id=modid:item;category=SCP Documents;name=SCP-330 Containment Protocol;image=scpinventory:textures/gui/documents/scp_330.png;text=scpinventory:codex/scp_330.txt;image_width=1279;image_height=1920\"]",
                        "Wrong example:",
                        "  codex_documents = [id=modid:item;category=SCP Documents;name=SCP-330 Containment Protocol]",
                        "Fields:",
                        "  id: required item id.",
                        "  category/type/section: left-panel group/tab name.",
                        "  name/display_name/title: document title shown inside the category.",
                        "  image/texture/photo: ResourceLocation for a static document image.",
                        "  text/transcript/transcription: ResourceLocation for a .txt transcription asset.",
                        "  image_width/image_height: source texture dimensions. Defaults to 1279x1920.",
                        "  creator/timestamp/uuid: optional Camerapture-style NBT filters kept for later integration.",
                        "  nbt_key/nbt_value: optional generic NBT filter for custom debug or special documents.",
                        "Legacy pipe format is still accepted:",
                        "  modid:item|Display Name|creator|timestamp|uuid",
                        "New pipe format is also accepted:",
                        "  modid:item|Category|Display Name|image_resource|text_resource|creator|timestamp|uuid|image_width|image_height",
                        "Debug note: /scpinventory debugcodex adds a built-in paper document for UI testing without editing this file."
                )
                .defineList("codex_documents", List.<String>of(), ScpInventoryConfig::isString);

        builder.pop();

        SPEC = builder.build();
    }

    private ScpInventoryConfig() {
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }
}
