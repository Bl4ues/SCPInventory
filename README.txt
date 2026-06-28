SCP Inventory
=============

SCP Inventory is a Forge 1.20.1 mod that replaces the normal pickup flow with a custom SCP/survival-horror inventory model.

Current architecture
--------------------

- Main inventory capability with 12 normal item slots by default.
- The maximum main slot count is stored in the player capability and can be changed later by backpack/upgrades.
- Each occupied main slot represents one real item.
- Main inventory pickup splits ItemStack counts into individual one-count slots.
- If the world item entity has count 1, it occupies 1 main slot.
- If the world item entity has count 2, it occupies 2 main slots.
- If a stack count is larger than the available free main slots, only the amount that fits is picked up.
- Same items are never merged inside the custom main inventory.
- Separate keyring storage for up to 12 key/keycard ItemStacks.
- Keyring items are mirrored into the player's vanilla inventory main area, never the hotbar, for compatibility with keycard/door mods that scan vanilla inventory contents.
- Separate Codex storage for document ItemStacks, preserving their NBT for image/document rendering.
- Equipment storage uses custom SCP slots: Head, Accessory, Body, and Weapon.
- Head and Body equipment are mirrored into the player's vanilla armor slots.
- Custom TAB screen with a scrollable item list, context menu, key tab, and functional equipment panel.
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
    KEY
    CODEX
    HEAD
    ACCESSORY
    BODY
    WEAPON

Examples:

    scp_additions:level_2_keycard|KEY
    scp_additions:gas_mask|HEAD
    scp_additions:scp_714|ACCESSORY
    minecraft:leather_chestplate|BODY
    minecraft:stick|WEAPON
    minecraft:golden_apple|CONSUMABLE

Items not listed in the config fall back to automatic detection:

- edible items become Consumable;
- vanilla helmets become Head;
- vanilla chestplates become Body;
- everything else becomes Miscellaneous.

Keyring
-------

Keyring capacity is currently fixed at 12 items.

A key item is stored in two places:

- the SCP keyring capability, used by the custom GUI;
- a mirrored copy inside vanilla inventory slots 9-35, used for compatibility with other mods.

Hotbar slots 0-8 are intentionally avoided.

Dropping a key from the KEYS tab removes the custom keyring entry and its mirrored vanilla inventory copy.

Equipment
---------

Planned/custom equipment slots:

    HEAD
    ACCESSORY
    BODY
    WEAPON

Left-click an occupied equipment slot in the current test GUI to unequip it back into the main inventory.
Right-click an occupied equipment slot to drop it into the world.

Weapon is currently stored as custom equipment only. It is not yet force-synced into the vanilla hand/hotbar.

Codex documents
---------------

Codex document rules use this recommended format:

    item_id|display_name|creator|timestamp|uuid

Example for CameraCapture-style pictures:

    camerapture:picture|SCP-330 Containment Protocol|Bl4ues|1772756289088L|I; 1717201316, -226147414, -127193371, 1090671268

The UUID field may contain semicolons because the parser splits this format by pipes.

A matching document does not consume one of the 12 main inventory slots. It is stored in the Codex as the original ItemStack, not as a plain string. This keeps creator/timestamp/uuid and any other NBT available for the future Codex GUI.

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
