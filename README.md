Chestpack - extra storage in a backpack on your chest! 

Need extra storage while traveling? Wear a chest on your chest!
Chestpack enables players to craft backpacks of various sizes for augmenting
their normal player inventory space. It is designed to enable carrying 
additional items realistically without being too overpowered.

**[Chestpack 1.1](http://dev.bukkit.org/server-mods/chestpack/files/2-chestpack-1-1/)** - released 2012/04/12 for 1.2.5

Features:

* No client mods required
* Crafting recipes to create packs
* Three different types, with configurable material and size
* Optional integrated workbench
* Packs are uniquely identified, and can be opened by id as an admin
* Equip pack as chestplate for storage (optional)
* Hold pack in your hand when picked up (optional)
* Nesting packs within packs is prevented (optional)
* Permissions support
* Somewhat realistic

[Forum thread](http://forums.bukkit.org/threads/mech-rpg-chestpack-v1-1-extra-storage-in-a-backpack-on-your-chest-1-2-5-r2-0.76470/)

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

autoHold: If enabled, packs will automatically be held in your hand if
placed in your hotbar or picked up from the ground. Along with autoEquip,
this option is meant to simulate a pack you can either hold in your hands
or wear on your back, but not keep in your "pockets" (other inventory slots).
Disable along with autoEquip if you want the pack to (less realistically) behave as a normal
item so you can carry multiple packs.

allowOpenContainers: Allows you to still open containers (chests, etc.) by right-clicking, instead of opening the pack.

equipSlot: Player inventory slot to wear the chestpack on if autoEquip enabled.
3 = helmet, 2 = chestplate, 1 = leggings, 0 = boots.

maxSlots: Number of slots to show with /chestpack command.

packTypes: A list of the packs to allow crafting for. Each item has several fields:

packTypes material: The material to craft the pack from (in addition to the required chest).

packTypes material\_count: Number of material items to require to craft.

packTypes size: Number of slots in the resulting chest. For reference 27 is a small chest, 54 is a large chest.

packTypes hasWorkbench: If true, the pack can be shift-clicked to open an integrated crafting table.

The default configuration is:

    allowNesting: false
    autoEquip: true
    autoHold: true
    equipSlot: 2
    maxSlots: 54
    packTypes:
        - {material: leather, material_count: 8, size: 45}
        - {material: wool, material_count: 8, size: 27, hasWorkbench: true}
        - {material: string, material_count: 4, size: 9}


Alternatively you can disable Chestpack's recipes and use another plugin to add more complex custom recipes.
Simply add a recipe to craft a chest with the following enchantments:

* Fortune I - identifies an empty pack (higher levels are used as unique identifiers)
* Efficiency # - level is number of slots, negated
* Punch - if present, pack has an integrated workbench

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
