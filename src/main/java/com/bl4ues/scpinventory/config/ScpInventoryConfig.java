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
                        "Readable example:",
                        "  item_rules = [",
                        "      \"minecraft:leather_helmet|HEAD\",",
                        "      \"minecraft:leather_chestplate|BODY\",",
                        "      \"minecraft:clock|ACCESSORY\",",
                        "      \"minecraft:stick|WEAPON\",",
                        "      \"minecraft:golden_apple|CONSUMABLE\",",
                        "      \"minecraft:tripwire_hook|KEY\"",
                        "  ]"
                )
                .defineList("item_rules", List.<String>of(), ScpInventoryConfig::isString);

        CODEX_DOCUMENTS = builder
                .comment(
                        "Codex document rules. Matching documents are unlocked into the Codex and do not consume main inventory slots.",
                        "IMPORTANT: this is a TOML string list. Each whole document rule must be wrapped in quotes.",
                        "One-line example:",
                        "  codex_documents = [\"id=modid:item;category=SCP Documents;name=SCP-330 Containment Protocol;image=scpinventory:textures/gui/documents/scp_330.png;text=scpinventory:codex/scp_330.txt;image_width=1279;image_height=1920\"]",
                        "Readable multiline example supported by the parser:",
                        "  codex_documents = [",
                        "      \"\"\"",
                        "      id=minecraft:paper",
                        "      category=SCP Documents",
                        "      name=Debug Image Document",
                        "      image=scpinventory:textures/gui/documents/debug_document.png",
                        "      text=scpinventory:codex/debug_paper.txt",
                        "      image_width=1279",
                        "      image_height=1920",
                        "      nbt_key=ScpCodexDebug",
                        "      nbt_value=true",
                        "      \"\"\"",
                        "  ]",
                        "Fields:",
                        "  id: required item id.",
                        "  category/type/section: left-panel group/tab name.",
                        "  name/display_name/title: document title shown inside the category.",
                        "  image/texture/photo: ResourceLocation for a static document image.",
                        "  text/transcript/transcription: ResourceLocation for a .txt transcription asset.",
                        "  image_width/image_height: source texture dimensions. Defaults to 1279x1920.",
                        "  creator/timestamp/uuid: optional Camerapture-style NBT filters kept for later integration.",
                        "  nbt_key/nbt_value: optional generic NBT filter for custom debug or special documents.",
                        "Debug note: /scpinventory debugcodex adds a built-in paper document for UI testing."
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
