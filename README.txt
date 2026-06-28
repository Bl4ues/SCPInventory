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
- If a stack count is larger than the available free main slots, it is not picked up.
- Same items are never merged inside the custom main inventory.
- Separate keyring storage for key/keycard ItemStacks.
- Separate Codex storage for document ItemStacks, preserving their NBT for image/document rendering.
- Equipment storage for Head, Body, Legs, and Feet.
- Custom TAB screen with a scrollable item list and context menu.
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
    BODY
    LEGS
    FEET

Examples:

    scp_additions:level_2_keycard|KEY
    scp_additions:gas_mask|HEAD
    minecraft:golden_apple|CONSUMABLE

Items not listed in the config fall back to automatic detection:

- edible items become Consumable;
- vanilla armor becomes its matching equipment slot;
- everything else becomes Miscellaneous.

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

    /scpinventory clearmain
    /scpinventory clearmain <targets>

Set max main slots for yourself:

    /scpinventory setmax <slots>

Set max main slots for selected players:

    /scpinventory setmax <targets> <slots>

Check your current occupied/max main slots:

    /scpinventory getmax

Development
-----------

Generate runs normally for ForgeGradle:

    ./gradlew genIntellijRuns
    ./gradlew runClient

Build:

    ./gradlew build
