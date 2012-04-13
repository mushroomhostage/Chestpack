Chestpack - extra storage in a backpack on your chest! 

Need extra storage while traveling? Wear a chest on your chest!
Chestpack enables players to craft backpacks of various sizes for augmenting
their normal player inventory space. It is designed to enable carrying 
additional items realistically without being too overpowered.

***[Chestpack 1.0 released](http://dev.bukkit.org/server-mods/chestpack/files/1-chestpack-1-0/)*** - requires 1.2.4+

Features:

* No client mods required
* Crafting recipes to create packs
* Three different types, with configurable material and size
* Optional integrated workbench
* Wear pack as chestplate for storage
* Hold pack in your hand to open it
* Packs are unique and not per-player
* Nesting packs within packs is prevented
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

autoEquip: If enabled, packs will automatically equip in your armor slot,
simulating wearing a chestpack, actively preventing you from storing the
pack elsewhere in your inventory. 

maxSlots: Number of slots to show with /chestpack command.

packTypes: A list of the packs to allow crafting for. Each item has several fields:

packTypes material: The material to craft the pack from (in addition to the required chest).

packTypes material\_count: Number of material items to require to craft.

packTypes size: Number of slots in the resulting chest. For reference 27 is a small chest, 54 is a large chest.

packTypes hasWorkbench: If true, the pack can be shift-clicked to open an integrated crafting table.

The default configuration is:

    allowNesting: false
    autoEqup: true
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

Chestpack inventories are stored in separate files, named pack<id>.yml.

## Permissions and Commands

chestpack.admin (op): Allows you to open any chestpack with the /chestpack command

chestpack.open.size.9 (true): Allows you to open chestpacks of size 9
    
chestpack.open.size.18 (true): Allows you to open chestpacks of size 18

chestpack.open.size.27 (true): Allows you to open chestpacks of size 27
    
chestpack.open.size.45 (true): Allows you to open chestpacks of size 45
    
chestpack.open.size.54 (true): Allows you to open chestpacks of size 54
    
chestpack.open.size.any (false): Allows you to open chestpacks of any size

chestpack.open.workbench (true): Allows you to open integrated workbenches


/chestpack id: Opens chestpack with the given id, 2 or greater (aliases: /pack, /cp).

## Caveats
The chest pack item shows up as a normal "Chest" in the player inventory. But its behavior
should make it clear how it differs (it auto-equips, can be opened, doesn't stack with other chests).

## See also

Other mods or plugins worth checking out:

* [Backpack](http://www.minecraftforum.net/topic/741100-123-backpack-ssp-smp/) - client/server mod with wearable backpacks, magic backpacks
* [Forestry](http://forestry.sengir.net/wiki/index.php?n=Items.Backpacks) - client/server mod with several item-specific backpacks

***[Fork me on GitHub](https://github.com/mushroomhostage/Chestpack)***
