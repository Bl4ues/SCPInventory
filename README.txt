SCP Inventory
=============

SCP Inventory is a Forge 1.20.1 mod that replaces the normal pickup flow with a custom SCP/survival-horror inventory model inspired by SCP: Unity.

Current architecture
--------------------

- Main inventory capability with 12 normal item slots by default.
- The maximum main slot count is stored in the player capability and can be changed by upgrades or commands.
- Each occupied main slot represents one real item.
- Main inventory pickup splits normal ItemStack counts into individual one-count slots.
- If a world item entity has count 1, it occupies 1 main slot.
- If a world item entity has count 2, it occupies 2 main slots.
- If a stack count is larger than the available free main slots, only the amount that fits is picked up.
- Same items are never merged inside the custom main inventory.
- Separate keyring storage for up to 12 key/keycard ItemStacks.
- Keyring items are mirrored into the player's vanilla inventory main area, never the hotbar, for compatibility with keycard/door mods that scan vanilla inventory contents.
- Separate Codex storage for document ItemStacks, preserving their NBT for image/document rendering.
- Equipment storage uses custom SCP slots: Head, Chest, Legs, Feet, Accessory, and Weapon.
- Head, Chest, Legs, and Feet equipment are mirrored into the player's vanilla armor slots.
- Accessory equipment has two modes: normal Accessory mirrors into the internal vanilla inventory, while AccessoryHand uses the player's offhand. Both still appear as the same Accessory slot in the GUI.
- Weapon equipment is mirrored into vanilla inventory/hotbar space for compatibility when possible.
- Coin items stay only in the vanilla inventory, stack normally, do not consume main/key slots, and are shown through a small counter in the custom inventory header.
- Custom TAB screen with Inventory, Status, and Codex pages.
- Inventory page includes a scrollable item list, context menu, Inventory/Keys tabs, equipment panel, drag-and-drop movement, and world-drop preview.
- Status page includes active conditions/effects, a 3D player preview, vanilla combat/armor parameters, and the client stamina display.
- Codex page includes grouped document browsing and image/transcription support.
- Server-authoritative item actions: the GUI requests actions, and the server mutates the capability.
- Server-to-client sync packet so the GUI sees the real inventory state.
- Inventory Full overlay is triggered through networking instead of client code being called from server events.

Config
------

Forge generates the common config at:

    config/scpinventory-common.toml

Item rules use this format:

    modid:item|TYPE

Accepted TYPE values:

    MISC
    CONSUMABLE
    USABLE
    KEY
    CODEX
    COIN
    HEAD
    ACCESSORY
    ACCESSORYHAND
    BODY
    CHEST
    LEGS
    FEET
    WEAPON

Examples:

    scp_additions:level_2_keycard|KEY
    scp_additions:gas_mask|HEAD
    scp_additions:hazmat_chestplate|CHEST
    scp_additions:hazmat_leggings|LEGS
    scp_additions:hazmat_boots|FEET
    scp_additions:scp_714|ACCESSORY
    scp_additions:offhand_detector|ACCESSORYHAND
    scp_additions:handheld_scanner|USABLE
    scp_additions:scp_coin|COIN
    minecraft:stick|WEAPON
    minecraft:golden_apple|CONSUMABLE

Items not listed in the config fall back to automatic detection:

- edible items become Consumable;
- potions and potion-like items become Consumable;
- vanilla helmets become Head;
- vanilla chestplates become Chest;
- vanilla leggings become Legs;
- vanilla boots become Feet;
- everything else becomes Miscellaneous.

Item categories
---------------

MISC
    Normal custom-inventory item. It occupies one main slot and can be dropped from the custom inventory.

CONSUMABLE
    Item that is used from the custom inventory and is consumed or removed. Vanilla food/drink/potion-style items run their normal finishUsingItem behavior. Non-vanilla consumables without a vanilla use result are simply removed from the custom inventory when used.

USABLE
    Item that must be right-clicked but should not be consumed directly by the custom inventory. Double-clicking it, or using the context menu USE option, closes the SCP inventory, moves the item into an empty hotbar slot, selects that slot, and simulates a main-hand right click. If no hotbar slot is available, the item remains in the custom inventory.

KEY
    Item stored in the SCP keyring and mirrored into vanilla inventory slots 9-35 for compatibility.

CODEX
    Item stored as a Codex document. Codex rules are configured through codex_documents and are checked before generic item_rules.

COIN
    Currency-like item. Configure only one item as COIN. Coin items are not inserted into the custom main inventory or keyring. They stay in the vanilla inventory, stack normally, remain visible to other mods/containers, and are counted by the GUI coin counter. Coin stacks are prevented from being tossed as normal dropped items.

HEAD / CHEST / LEGS / FEET / ACCESSORY / ACCESSORYHAND / WEAPON
    Equipment categories. ACCESSORY and ACCESSORYHAND both display as Accessory and both use the same SCP equipment slot; the difference is only the vanilla mirror target.

Keyring
-------

Keyring capacity is currently fixed at 12 items.

A key item is stored in two places:

- the SCP keyring capability, used by the custom GUI;
- a mirrored copy inside vanilla inventory slots 9-35, used for compatibility with other mods.

Hotbar slots 0-8 are intentionally avoided for keys.

Dropping a key from the KEYS tab removes the custom keyring entry and its mirrored vanilla inventory copy.

If a mirrored key is removed from vanilla inventory slots 9-35, the SCP keyring entry should be treated as stale and removed by synchronization logic. If a configured key appears in vanilla inventory slots 9-35, it can be registered into the SCP keyring, up to the 12-key limit.

Coins
-----

Coins are intentionally different from keys:

- they are not duplicated into the custom inventory capability;
- they do not count toward main inventory capacity;
- they do not count toward keyring capacity;
- they remain as normal stackable vanilla inventory items;
- they can be used by other mods and containers that inspect or consume vanilla inventory contents;
- the GUI counter reads the player's vanilla inventory, so external removals/additions update the displayed count.

Only one configured COIN item is supported. If multiple item_rules use COIN, the first configured rule is used for the counter icon and count target.

Equipment
---------

Current equipment slots:

    HEAD
    CHEST
    LEGS
    FEET
    ACCESSORY
    WEAPON

Left-click and drag equipment to move it. Double-click an occupied equipment slot to unequip it back into the main inventory. Dragging outside the GUI drops the item into the world when the category allows it.

Head, Chest, Legs, and Feet are mirrored into vanilla armor slots.

Accessory has two config modes:

    ACCESSORY
        The item appears in the SCP Accessory slot and is mirrored into the internal vanilla inventory.

    ACCESSORYHAND
        The item appears in the same SCP Accessory slot but is mirrored into the player's offhand.
        The aliases ACCESSORY_HAND, ACCESSORYOFFHAND, ACCESSORY_OFFHAND, OFFHAND_ACCESSORY, ACCESORYHAND, and ACCESORY_HAND are also accepted.

Weapon equipment is mirrored into vanilla inventory/hotbar space when possible.

Codex documents
---------------

Codex document rules use the newer key/value format:

    id=modid:item;category=SCP Documents;name=SCP-330 Containment Protocol;image=scpinventory:textures/gui/documents/scp_330.png;text=scpinventory:codex/scp_330.txt;image_width=1279;image_height=1920

Readable multiline TOML example:

    codex_documents = [
        """
        id=modid:item
        category=SCP Documents
        name=SCP-330 Containment Protocol
        image=scpinventory:textures/gui/documents/scp_330.png
        text=scpinventory:codex/scp_330.txt
        image_width=1279
        image_height=1920
        """
    ]

Supported fields:

    id
        Required item id.

    category / type / section
        Left-panel group/tab name.

    name / display_name / title
        Document title shown inside the category.

    image / texture / photo
        ResourceLocation for a static document image. Default page size is 1279x1920.

    text / transcript / transcription
        ResourceLocation for a .txt transcription asset.

    image_width / image_height
        Source texture dimensions. Defaults to 1279x1920.

    creator / timestamp / uuid
        Optional Camerapture-style NBT filters kept for later integration.

    nbt_key / nbt_value
        Optional generic NBT filter for custom documents.

A matching document does not consume one of the 12 main inventory slots. It is stored in the Codex as the original ItemStack, not as a plain string. This keeps creator/timestamp/uuid and any other NBT available for the Codex GUI.

Debug commands
--------------

These commands require permission level 2.

Reset everything for yourself:

    /scpinventory reset

Reset everything for selected players:

    /scpinventory reset <targets>

Clear only the main inventory, keeping keys, documents, equipment, and current max slots:

    /scpinventory clear
    /scpinventory clear <targets>
    /scpinventory clearmain
    /scpinventory clearmain <targets>

Set max main slots for yourself:

    /scpinventory setmax <slots>
    /scpinventory maxslots <slots>

Set max main slots for selected players:

    /scpinventory setmax <targets> <slots>
    /scpinventory maxslots <targets> <slots>

Check your current occupied/max main slots and key count:

    /scpinventory getmax

Development
-----------

Generate runs normally for ForgeGradle:

    ./gradlew genIntellijRuns
    ./gradlew runClient

Build:

    ./gradlew build
