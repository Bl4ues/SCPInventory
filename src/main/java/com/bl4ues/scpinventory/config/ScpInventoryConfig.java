package com.bl4ues.scpinventory.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ScpInventoryConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_RULES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CODEX_DOCUMENTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HIDDEN_STATUS_EFFECTS;

    private static final List<String> DEFAULT_CODEX_DOCUMENTS = List.of(
            "id=minecraft:paper;category=Debug Single;name=Debug Entry Alpha;image=scpinventory:textures/gui/health.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:book;category=Debug Pair;name=Debug Entry Beta;image=scpinventory:textures/gui/inventoryicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:writable_book;category=Debug Pair;name=Debug Entry Gamma;image=scpinventory:textures/gui/statusicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:map;category=Debug Trio;name=Debug Entry Delta;image=scpinventory:textures/gui/codexicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:compass;category=Debug Trio;name=Debug Entry Epsilon;image=scpinventory:textures/gui/inventory_background.png;text=scpinventory:codex/debug_paper_long.txt;image_width=1406;image_height=1080",
            "id=minecraft:clock;category=Debug Trio;name=Debug Entry Zeta;image=scpinventory:textures/gui/codexicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128"
    );

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("scp_inventory");

        ITEM_RULES = builder
                .comment(
                        "Item classification rules used by the SCP inventory.",
                        "Format: modid:item|TYPE",
                        "Accepted TYPE values: MISC, CONSUMABLE, USABLE, KEY, CODEX, COIN, HEAD, BODY, CHEST, LEGS, FEET, ACCESSORY, ACCESSORYHAND, WEAPON.",
                        "USABLE items behave like right-click items that are not consumed by the SCP inventory: using one from the custom GUI moves it into the selected hotbar flow and simulates main-hand right click.",
                        "ACCESSORY and ACCESSORYHAND are displayed as the same Accessory equipment slot. ACCESSORY mirrors into the internal vanilla inventory; ACCESSORYHAND uses the player's offhand.",
                        "COIN is special: configure only ONE item as COIN. Coins stay only in the vanilla inventory, stack normally, do not consume main/key slots, are counted in the GUI counter, and are not listed as custom inventory entries.",
                        "Items not listed here fall back to vanilla detection: food becomes Consumable, vanilla armor goes to its matching equipment slot, everything else becomes Miscellaneous.",
                        "Readable example:",
                        "  item_rules = [",
                        "      \"scp_additions:level_2_keycard|KEY\",",
                        "      \"scp_additions:gas_mask|HEAD\",",
                        "      \"scp_additions:scp_714|ACCESSORY\",",
                        "      \"scp_additions:handheld_scanner|USABLE\",",
                        "      \"scp_additions:offhand_detector|ACCESSORYHAND\",",
                        "      \"scp_additions:scp_coin|COIN\",",
                        "      \"minecraft:golden_apple|CONSUMABLE\"",
                        "  ]"
                )
                .defineList("item_rules", List.<String>of(), ScpInventoryConfig::isString);

        HIDDEN_STATUS_EFFECTS = builder
                .comment(
                        "Mob effects that should not appear in the STATUS_CONDITIONS screen.",
                        "Format: modid:effect_id",
                        "Default includes minecraft:bad_omen so it can be tested immediately.",
                        "Example:",
                        "  hidden_status_effects = [\"minecraft:bad_omen\", \"scpo:pestilence\"]"
                )
                .defineList("hidden_status_effects", List.<String>of("minecraft:bad_omen"), ScpInventoryConfig::isString);

        CODEX_DOCUMENTS = builder
                .comment(
                        "Codex document rules. Matching documents are unlocked into the Codex and do not consume main inventory slots.",
                        "The default list contains six debug/sample documents: one in Debug Single, two in Debug Pair, and three in Debug Trio.",
                        "IMPORTANT: this is a TOML string list. Each whole document rule must be wrapped in quotes.",
                        "One-line example:",
                        "  codex_documents = [\"id=modid:item;category=SCP Documents;name=SCP-330 Containment Protocol;image=scpinventory:textures/gui/documents/scp_330.png;text=scpinventory:codex/scp_330.txt;image_width=1279;image_height=1920\"]",
                        "Readable multiline example supported by the parser:",
                        "  codex_documents = [",
                        "      \"\"\"",
                        "      id=modid:item",
                        "      category=SCP Documents",
                        "      name=SCP-330 Containment Protocol",
                        "      image=scpinventory:textures/gui/documents/scp_330.png",
                        "      text=scpinventory:codex/scp_330.txt",
                        "      image_width=1279",
                        "      image_height=1920",
                        "      \"\"\"",
                        "  ]",
                        "Fields:",
                        "  id: required item id.",
                        "  category/type/section: left-panel group/tab name.",
                        "  name/display_name/title: document title shown inside the category.",
                        "  image/texture/photo: ResourceLocation for a static document image. Default document page size is 1279x1920.",
                        "  text/transcript/transcription: ResourceLocation for a .txt transcription asset.",
                        "  image_width/image_height: source texture dimensions. Defaults to 1279x1920.",
                        "  creator/timestamp/uuid: optional Camerapture-style NBT filters kept for later integration.",
                        "  nbt_key/nbt_value: optional generic NBT filter for custom documents."
                )
                .defineList("codex_documents", DEFAULT_CODEX_DOCUMENTS, ScpInventoryConfig::isString);

        builder.pop();

        SPEC = builder.build();
    }

    private ScpInventoryConfig() {
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }
}
