package me.exphc.Chestpack;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Formatter;
import java.lang.Byte;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.Material.*;
import org.bukkit.material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.scheduler.*;
import org.bukkit.enchantments.*;
import org.bukkit.inventory.*;
import org.bukkit.*;

import net.minecraft.server.CraftingManager;

class ChestpackListener implements Listener {
    Chestpack plugin;

    public ChestpackListener(Chestpack plugin) {
        this.plugin = plugin;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        //player.getInventory().setChestplate(new ItemStack(Material.CHEST, 1));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        Action action = event.getAction();

        if (isPack(item)) {
            //if (action == Action.LEFT_CLICK_AIR || action == action.LEFT_CLICK_BLOCK) {
            // TODO: left-click to open, right-click to place (set down as chest)

            // left-click to open
            openPack(player, item);

            event.setCancelled(true);
        } else if (isPortableWorkbench(item)) {
            player.openWorkbench(player.getLocation(), true);
            event.setCancelled(true);
        }
    }

    private void openPack(Player player, ItemStack item) {
        int numSlots = getPackSize(item);

        int id = getPackId(item);
        if (id == 1) {
            // New blank pack
            id = newPackId();

            item.addUnsafeEnchantment(FORTUNE, id);
        }

        Inventory inventory = Bukkit.createInventory(player, numSlots, "Backpack " + id);

        loadPack(id, inventory);

        InventoryView view = player.openInventory(inventory);
        // will be saved on close
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onInventoryClose(InventoryCloseEvent event) {
        // Anytime rearrange inventory, have to check if moved to/from chestplate/hand/other
        for (HumanEntity viewer: event.getViewers()) {
            if (!(viewer instanceof Player)) {
                continue;
            }

            // crafted a backpack? wear it!
            checkEquipPack((Player)viewer);
        }

        // Save pack on close
        String title = event.getView().getTitle();
        if (title.startsWith("Backpack")) {
            int id = Integer.parseInt(title.replace("Backpack ", "")); // :(

            // TODO: should we prevent nested backpacks?

            // Note: broken on 1.2.3-R0.2. Works on 1.2.4 snapshots: craftbukkit-1.2.4-R0.1-20120325.235512-21.jar
            savePack(id, event.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        int previousSlot = event.getPreviousSlot();
        if (isPack(player.getInventory().getContents()[previousSlot])) {
            // backpack was held in hand, then switched off of it = wear it
            // TODO: other options? drop? or only drop if e.g. sneaking? (can always 'Q' = drop, though)
            equipPack(player, previousSlot);
        }
    }

    // TODO: why broken?
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        // picked up a backpack item? (TODO: prevent?)
        checkEquipPack(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item itemEntity = event.getItemDrop();
        ItemStack itemStack = itemEntity.getItemStack();

        if (!isPack(itemStack)) {
            return;
        }
        
        if (shouldDropAsItem()) {
            // disable chest drops
            return;
        }

        Player player = event.getPlayer();

        boolean dropped = dropPack(player, itemStack);
        if (!dropped) {
            // failed to drop..we can't have this lingering around as an item..return to player
            // TODO: test better
            event.setCancelled(true);
        } else {
            itemEntity.remove();
        }
    }


    // DROPPING AND EQUIPPING

    private boolean shouldDropAsItem() {
        return plugin.getConfig().getBoolean("dropAsItem", true);
    }

    /** Drop a backpack item into the physical world as a chest block.
    @return Whether backpack was successfully dropped
    */
    private boolean dropPack(Player player, ItemStack item) {
        // Find out where to drop backpack as a block
        Block block = player.getTargetBlock(null, 5).getRelative(BlockFace.UP);
        // TODO: get face of targetted block
        // TODO: only place in air
        // TODO: avoid placing adjacent to another chest to make a doublechest..
        if (block == null) {
            // TODO: set in a more reasonable location?
            //block = itemEntity.getLocation().getBlock();
            player.sendMessage("Unable to drop backpack here");
            return false;
        }

        player.sendMessage("Backpack dropped: " + getPackDisplayName(item));

        // TODO: respect world protection
        block.setTypeIdAndData(Material.CHEST.getId(), (byte)0, true);
        // since backpacks can be larger, need a double chest
        block.getRelative(BlockFace.NORTH).setTypeIdAndData(Material.CHEST.getId(), (byte)0, true);

        BlockState blockState = block.getState();
        if (!(blockState instanceof Chest)) {
            plugin.log.info("Failed to find dropped chest");
            return true;    // so don't dupe
        }

        Chest chest = (Chest)blockState;

        plugin.log.info("chest="+chest);

        Inventory inventory = chest.getBlockInventory();
        if (!(inventory instanceof DoubleChestInventory)) {
            plugin.log.info("Failed to find double chest inventory");
            return true;
        }
        DoubleChestInventory chestInventory = (DoubleChestInventory)inventory;

        plugin.log.info("dci="+chestInventory);


        // TODO: identify as pack
        // TODO: populate contents

        return true;
    }

    /** Wear a player's pack in their chestplate slot, if they have one in their inventory.
    */
    private void checkEquipPack(Player player) {
        ItemStack[] inventory = player.getInventory().getContents();

        for (int i = 0; i < inventory.length; i += 1) {
            ItemStack item = inventory[i];

            if (item == null || item.equals(player.getItemInHand())) {
                // You're allowed to hold backpacks in your hand (or on your back, but nowhere else)
                continue;
            }

            if (isPack(item)) {
                final int HOTBAR_SIZE = 9;
                if (i < HOTBAR_SIZE) {
                    // Placed in hotbar, so move it to item held
                    // TODO: can we change held slot? guess not
                    ItemStack old = player.getItemInHand();
                    player.setItemInHand(item);
                    player.getInventory().setItem(i, old);
                } else {
                    // Placed elsewhere in inventory, so wear it
                    equipPack(player, i);
                }
            }
        }
    }

    /** Wear a backpack, transferring from existing inventory slot. 
    */
    private void equipPack(Player player, int slot) {
        // existing armor falls off
        ItemStack old = player.getInventory().getChestplate();
        if (old != null) {
            if (isPack(old) && !shouldDropAsItem()) {
                // if dropping another backpack, place on ground
                dropPack(player, old);
            } else {
                // simply fall to ground (probably will pickup as item)
                player.getWorld().dropItemNaturally(player.getLocation(), old);
            }
        }

        // equip pack
        ItemStack item = player.getInventory().getContents()[slot];
        player.getInventory().setChestplate(item);
        player.getInventory().clear(slot);

        player.sendMessage("Backpack equipped: " + getPackDisplayName(item));
    }

    /// IDENTIFYING, LOADING, AND SAVING

    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;
    final Enchantment EFFICIENCY = Enchantment.DIG_SPEED;

    /** Get whether the item is a pack.
    */
    private boolean isPack(ItemStack item) {
        // Represented by a chest with a special enchantment
        return item != null && item.getType() == Material.CHEST && item.containsEnchantment(FORTUNE);
    }

    private boolean isPortableWorkbench(ItemStack item) {
        return item != null && item.getType() == Material.WORKBENCH && item.containsEnchantment(EFFICIENCY);
    }

    /** Get the identifier for the pack (1 = empty, greater than 1 = uniquely assigned)
    */
    private int getPackId(ItemStack item) {
        return item.getEnchantmentLevel(FORTUNE);
    }

    /** Get the size of a pack, in number of slots it can hold.
    */
    private int getPackSize(ItemStack item) {
        // Size of pack, must be multiple of 9. Large chest = 54=6*9
        // >54 glitches client UI, but <54 is fine. 45=5*9=Slightly smaller.
        return item.getEnchantmentLevel(EFFICIENCY);
    }

    private int newPackId() {
        int next = plugin.getConfig().getInt("nextId", 2);

        plugin.getConfig().set("nextId", next + 1);
        plugin.saveConfig();

        return next;
    }

    /** Get a textual 'name' of the backpack for display purposes. */
    private String getPackDisplayName(ItemStack item) {
        int level = getPackId(item);
        switch (level) {
        case 0: return "???";       // should not happen
        case 1: return "empty";     // uninitialized
        default: return "#" + level;
        }
        // TODO: arbitrarily assignable string names?
        // TODO: include player name (creator) in pack name?
    }


    /** Save contents of pack to disk. */
    private void savePack(int id, Inventory inventory) {
        plugin.getConfig().set("inventory."+id, inventory.getContents());
        plugin.saveConfig();
    }

    /** Load contents of pack from disk. */
    @SuppressWarnings("unchecked")
    private void loadPack(int id, Inventory inventory) {
        plugin.reloadConfig();

        List<?> list = plugin.getConfig().getList("inventory."+id);

        if (list != null) {
            for (int i = 0; i < list.size(); i += 1) {
                inventory.setItem(i, (ItemStack)list.get(i));
            }

            //inventory.setContents(contents);
        }
    }

}

public class Chestpack extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();

        loadBenchRecipe();
        loadPackRecipes();

        new ChestpackListener(this);
    }

    public void onDisable() {
        Bukkit.resetRecipes();  // TODO: can we reset only ours?
    }

    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;
    final Enchantment EFFICIENCY = Enchantment.DIG_SPEED;

    private void loadBenchRecipe() {
        Material material = Material.matchMaterial(getConfig().getString("portableWorkbench.material", "leather"));
        if (material == null) {
            return;
        }

        int count = getConfig().getInt("portableWorkbench.count", 8);

        ItemStack portableBench = new ItemStack(Material.WORKBENCH);
        portableBench.addUnsafeEnchantment(EFFICIENCY, 1);

        ShapelessRecipe recipe = new ShapelessRecipe(portableBench);
        recipe.addIngredient(count, material, -1);
        recipe.addIngredient(1, Material.WORKBENCH);
        Bukkit.addRecipe(recipe);
    }

    private void loadPackRecipes() {
        // Crafting recipes for packs of different types
        List<Map<?,?>> maps = getConfig().getMapList("packTypes");
        for (Map<?,?> map: maps) {
            Material material = Material.matchMaterial((String)map.get("material"));
            if (material == null) {
                log.warning("Invalid material "+material+", ignored");
                continue;
            }
            int count = ((Integer)map.get("material_count")).intValue();
            int size = ((Integer)map.get("size")).intValue();

            log.info("Recipe "+material+" x "+count+" = "+size);

            ItemStack emptyPack = new ItemStack(Material.CHEST, 1);
            emptyPack.addUnsafeEnchantment(FORTUNE, 1);  // blank

            emptyPack.addUnsafeEnchantment(EFFICIENCY, size);

            /* // TODO: isolate why shaped recipes still lose on 1.2.3-R0.2
            https://bukkit.atlassian.net/browse/BUKKIT-602
            ShapedRecipe recipe = new ShapedRecipe(emptyPack);
            recipe.shape(
                "LLL",
                "LCL",
                "LLL");
            recipe.setIngredient('L', Material.LEATHER);
            recipe.setIngredient('C', Material.CHEST);
            */

            // as a workaround, do shapeless instead

            ShapelessRecipe recipe = new ShapelessRecipe(emptyPack);
            recipe.addIngredient(count, material, -1);
            recipe.addIngredient(1, Material.CHEST);

            Bukkit.addRecipe(recipe);
        }
    }
}
