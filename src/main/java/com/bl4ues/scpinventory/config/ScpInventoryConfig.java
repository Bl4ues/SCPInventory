package com.bl4ues.scpinventory.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ScpInventoryConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_RULES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CODEX_DOCUMENTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HIDDEN_STATUS_EFFECTS;

    private static final List<String> DEFAULT_ITEM_RULES = List.of(
            "minecraft:flint|COIN",
            "minecraft:leather|ACCESSORYHAND",
            "minecraft:clock|ACCESSORY",
            "minecraft:fishing_rod|USABLE",
            "minecraft:spyglass|USABLE",
            "minecraft:bow|USABLE",
            "minecraft:crossbow|USABLE",
            "minecraft:shield|USABLE",
            "minecraft:goat_horn|USABLE",
            "minecraft:ender_pearl|USABLE",
            "minecraft:snowball|USABLE",
            "minecraft:egg|USABLE"
    );

    private static final List<String> DEFAULT_CODEX_DOCUMENTS = List.of(
            "id=minecraft:paper;category=Debug Single;name=Debug Entry Alpha;image=scpinventory:textures/gui/health.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:book;category=Debug Pair;name=Debug Entry Beta;image=scpinventory:textures/gui/inventoryicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:writable_book;category=Debug Pair;name=Debug Entry Gamma;image=scpinventory:textures/gui/statusicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:map;category=Debug Trio;name=Debug Entry Delta;image=scpinventory:textures/gui/codexicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:compass;category=Debug Trio;name=Debug Entry Epsilon;image=scpinventory:textures/gui/inventory_background.png;text=scpinventory:codex/debug_paper_long.txt;image_width=1406;image_height=1080",
            "id=minecraft:clock;category=Debug Trio;name=Debug Entry Zeta;image=scpinventory:textures/gui/codexicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:name_tag;category=Debug Scroll A;name=Debug Entry Eta;image=scpinventory:textures/gui/inventoryicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:feather;category=Debug Scroll A;name=Debug Entry Theta;image=scpinventory:textures/gui/statusicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:bone;category=Debug Scroll A;name=Debug Entry Iota;image=scpinventory:textures/gui/codexicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:blaze_rod;category=Debug Scroll A;name=Debug Entry Kappa;image=scpinventory:textures/gui/health.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:amethyst_shard;category=Debug Scroll B;name=Debug Entry Lambda;image=scpinventory:textures/gui/inventoryicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:echo_shard;category=Debug Scroll B;name=Debug Entry Mu;image=scpinventory:textures/gui/statusicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:prismarine_shard;category=Debug Scroll B;name=Debug Entry Nu;image=scpinventory:textures/gui/codexicon.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:nether_star;category=Debug Scroll B;name=Debug Entry Xi;image=scpinventory:textures/gui/codexicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:heart_of_the_sea;category=Debug Scroll C;name=Debug Entry Omicron;image=scpinventory:textures/gui/inventory_background.png;text=scpinventory:codex/debug_paper_long.txt;image_width=1406;image_height=1080",
            "id=minecraft:rabbit_foot;category=Debug Scroll C;name=Debug Entry Pi;image=scpinventory:textures/gui/inventoryicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:nautilus_shell;category=Debug Scroll C;name=Debug Entry Rho;image=scpinventory:textures/gui/statusicon_selected.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128",
            "id=minecraft:recovery_compass;category=Debug Scroll C;name=Debug Entry Sigma;image=scpinventory:textures/gui/health.png;text=scpinventory:codex/debug_paper_long.txt;image_width=128;image_height=128"
    );

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("scp_inventory");

        ITEM_RULES = builder
                .comment(
                        "Item classification rules used by the SCP inventory.",
                        "Format: modid:item|TYPE",
                        "Accepted TYPE values: MISC, CONSUMABLE, USABLE, KEY, CODEX, COIN, HEAD, BODY, CHEST, LEGS, FEET, ACCESSORY, ACCESSORYHAND, WEAPON.",
                        "The default debug list uses flint as COIN so it behaves like a normal miscellaneous item while testing.",
                        "COIN behaves like a normal main SCP inventory item, counts against the main inventory slot limit, and is mirrored into vanilla inventory with an internal marker.",
                        "USABLE items move into the selected hotbar flow and simulate main-hand right click.",
                        "ACCESSORY and ACCESSORYHAND are displayed as the same Accessory slot. ACCESSORYHAND uses the player's offhand.",
                        "Items not listed here fall back to vanilla detection."
                )
                .defineList("item_rules", DEFAULT_ITEM_RULES, ScpInventoryConfig::isString);

        HIDDEN_STATUS_EFFECTS = builder
                .comment("Mob effects that should not appear in the STATUS_CONDITIONS screen.")
                .defineList("hidden_status_effects", List.<String>of("minecraft:bad_omen"), ScpInventoryConfig::isString);

        CODEX_DOCUMENTS = builder
                .comment("Codex document rules. Matching documents are unlocked into the Codex and do not consume main inventory slots.")
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
