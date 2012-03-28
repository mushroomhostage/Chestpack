Chestpack - extra storage in a backpack on your chest! 

Need extra storage while traveling? Wear a chest on your chest!
Chestpack enables players to craft backpacks of various sizes for augmenting
their normal player inventory space. It is designed to enable carrying 
additional items realistically without being too overpowered.

Features:

* Crafting recipes to create packs
* Three different types, with configurable material and size
* Optional integrated workbench
* Wear pack as chestplate for storage
* Hold pack in your hand to open it
* Packs are unique and not per-player
* Nesting packs within packs is prevented (can be enabled)
* Somewhat realistic

## Usage
Craft a pack by surrounding a chest with one of:

 * 4 string = 9-slot pack (daypack)
 * 8 wool = 27-slot pack + integrated workbench (craftsman's backpack)
 * 8 leather = 45-slot pack (explorer's backpack)

When holding the resulting chest (it appears the same as a normal chest, but is
actually a special wearable chest), click to open the pack. You can drag items
to/from the pack and your inventory, as you would expect.

If your pack has an integrated workbench, shift-click to open the crafting area.

When it isn't held in your hand, the pack will move itself to your
chestplate armor slot, representing the pack being worn on your back as a backpack
(or on your chest as a chestpack, whatever interpretation you prefer). This is where
the pack normally resides.

To get back into your pack, take it off your back and move it into your hand. Left-click
to open it. 

## Configuration

allowNesting: If enabled, packs can be stored within packs. When disabled
(the default), attempts to nest packs will drop them to the ground. 

maxSlots: Number of slots to show with /chestpack command.

packTypes: A list of the packs to allow crafting for. Each item has several fields:

packTypes material: The material to craft the pack from (in addition to the required chest).

packTypes material\_count: Number of material items to require to craft.

packTypes size: Number of slots in the resulting chest. For reference 27 is a small chest, 54 is a large chest.

packTypes hasWorkbench: If true, the pack can be shift-clicked to open an integrated crafting table.

The default configuration is:

    allowNesting: false
    maxSlots: 54
    packTypes:
        - {material: leather, material_count: 8, size: 45}
        - {material: wool, material_count: 8, size: 27, hasWorkbench: true}
        - {material: string, material_count: 4, size: 9}


Alternatively you can disable Chestpack's recipes and use another plugin to add more complex custom recipes.
Simply add a recipe to craft a chest with the following enchantments:

* Fortune I - identifies an empty pack (higher levels are used as unique identifiers)
* Efficiency # - level is number of slots, negated
* Power - if present, pack has an integrated workbench


## Permissions and Commands

chestpack.admin (op): Allows you to open any chestpack with the /chestpack command

/chestpack id: Opens chestpack with the given id, 2 or greater (aliases: /pack, /cp).

## Caveats
The chest pack item shows up as a normal "Chest" in the player inventory. But its behavior
should make it clear how it differs (it auto-equips, can be opened, doesn't stack with other chests).



