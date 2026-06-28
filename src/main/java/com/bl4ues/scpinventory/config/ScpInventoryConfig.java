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
                        "Accepted TYPE values: MISC, CONSUMABLE, KEY, CODEX, HEAD, ACCESSORY, BODY, WEAPON.",
                        "Items not listed here fall back to vanilla detection: food becomes Consumable, helmets become Head, chestplates become Body, everything else becomes Miscellaneous.",
                        "Examples:",
                        "  scp_additions:level_2_keycard|KEY",
                        "  scp_additions:gas_mask|HEAD",
                        "  scp_additions:scp_714|ACCESSORY",
                        "  minecraft:golden_apple|CONSUMABLE"
                )
                .defineList("item_rules", List.<String>of(), ScpInventoryConfig::isString);

        CODEX_DOCUMENTS = builder
                .comment(
                        "Codex document rules. Matching documents are unlocked into the Codex and do not consume main inventory slots.",
                        "Recommended format: item_id|display_name|creator|timestamp|uuid",
                        "Only item_id is mandatory. Leave optional fields empty when you do not need NBT matching.",
                        "The uuid field may contain semicolons because the parser splits this format by pipes.",
                        "Example:",
                        "  camerapture:picture|SCP-330 Containment Protocol|Bl4ues|1772756289088L|I; 1717201316, -226147414, -127193371, 1090671268",
                        "Also accepted: id=camerapture:picture;name=SCP-330 Containment Protocol;creator=Bl4ues;timestamp=1772756289088L;uuid=I; 1717201316, -226147414, -127193371, 1090671268"
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
