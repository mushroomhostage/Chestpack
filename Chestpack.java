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

class ChestpackListener implements Listener {
    static Chestpack plugin;

    public ChestpackListener(Chestpack plugin) {
        this.plugin = plugin;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        //player.getInventory().setChestplate(new ItemStack(plugin.chestpackItem, 1));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isPack(item)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && plugin.getConfig().getBoolean("allowOpenContainers", true)) {
            Block block = event.getClickedBlock();
            if (block != null) {
                BlockState blockState = block.getState();
                if (blockState instanceof InventoryHolder) {
                    // if right-clicked a container, let it open instead of taking over
                    return;
                }
            }
        }

        // always cancel the event to prevent normal interaction
        event.setCancelled(true);

        // sneaking opens workbench, if pack has one
        if (hasIntegratedWorkbench(item) && player.isSneaking()) {
            if (!player.hasPermission("chestpack.open.workbench")) {
                player.sendMessage("You do not have permission to open the integrated workbench");
                return;
            }

            // TODO: can we save/load the workbench inventory, so you can partially craft
            // something then return to it later? that would be cool. like BC2 Automatic Crafting Table
            player.openWorkbench(player.getLocation(), true);
            return;
        }

        //Action action = event.getAction();
        //if (action == Action.LEFT_CLICK_AIR || action == action.LEFT_CLICK_BLOCK) {
        // TODO: left-click to open, right-click to place (set down as chest)

        // open 
        int numSlots = getPackSize(item);
        if (!player.hasPermission("chestpack.open.size." + numSlots) && !player.hasPermission("chestpack.open.size.any")) {
            player.sendMessage("You do not have permission to open chestpacks of size "+numSlots);
            return;
        }

        openPack(player, item);
    }

    private void openPack(Player player, ItemStack item) {
        int id = getPackId(item);
        if (id == 1) {
            // New blank pack
            id = newPackId();

            item.addUnsafeEnchantment(FORTUNE, id);
        }

        int numSlots = getPackSize(item);

        openPack(player, id, numSlots);
    }

    public static void openPack(Player player, int id, int numSlots) {
        Inventory inventory = Bukkit.createInventory(player, numSlots, "Backpack " + id);

        loadPack(id, inventory);

        InventoryView view = player.openInventory(inventory);
        // will be saved on close
    }



    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getViewers() == null) {
            return;
        }

        // Anytime rearrange inventory, have to check if moved to/from chestplate/hand/other
        for (HumanEntity viewer: event.getViewers()) {
            if (!(viewer instanceof Player)) {
                continue;
            }

            // crafted a backpack? wear it!
            checkEquipPack((Player)viewer);
        }

        // Save pack on close
        if (event.getView() == null || event.getView().getTitle() == null) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.startsWith("Backpack")) {
            int id = Integer.parseInt(title.replace("Backpack ", "")); // :(

            Inventory inventory = event.getInventory();

            if (!plugin.getConfig().getBoolean("allowNesting", false)) {
                // If tried to put a pack within a pack.. pop it out
                ItemStack[] contents = inventory.getContents();
                for (int i = 0; i < contents.length; i += 1) {
                    if (isPack(contents[i])) {
                        // a pack within a pack..
                        // TODO: separately check if is SAME pack within itself (heh, its IS possible)
                        HumanEntity firstViewer = event.getViewers().get(0);
                        firstViewer.getWorld().dropItemNaturally(firstViewer.getLocation(), contents[i]);

                        inventory.setItem(i, null);
                    }
                }
            }

            // Note: broken on 1.2.3-R0.2. Works on 1.2.4 snapshots: craftbukkit-1.2.4-R0.1-20120325.235512-21.jar
            savePack(id, inventory);
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();

        if (isPack(itemStack)) {
            event.getPlayer().sendMessage("Backpack picked up: " + getPackDisplayName(itemStack));
        }

        // Equip or hold the pack picked up 
        // - we must do this after the event is completed, so we can look at the new inventory
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                checkEquipPack(player);
            }
        });
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
        block.setTypeIdAndData(plugin.chestpackItem.getId(), (byte)0, true);
        // since backpacks can be larger, need a double chest
        block.getRelative(BlockFace.NORTH).setTypeIdAndData(plugin.chestpackItem.getId(), (byte)0, true);

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
                    if (plugin.getConfig().getBoolean("autoHold", true)) {
                        // Placed in hotbar, so move it to item held
                        // TODO: can we change held slot? guess not
                        ItemStack old = player.getItemInHand();
                        player.setItemInHand(item);
                        player.getInventory().setItem(i, old);
                    }
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
        if (!plugin.getConfig().getBoolean("autoEquip", true)) {
            return; 
        }

        // existing armor falls off
        ItemStack old;
        switch (plugin.getConfig().getInt("equipSlot", 2)) {
        // standard inventory slot number minus 100 http://www.minecraftwiki.net/wiki/Data_values#Inventory_Slot_Number
        case 3: old = player.getInventory().getHelmet(); break;
        default:
        case 2: old = player.getInventory().getChestplate(); break;
        case 1: old = player.getInventory().getLeggings(); break;
        case 0: old = player.getInventory().getBoots(); break;
        }

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
        switch (plugin.getConfig().getInt("equipSlot", 2)) {
        case 3: player.getInventory().setHelmet(item); break;
        default:
        case 2: player.getInventory().setChestplate(item); break;
        case 1: player.getInventory().setLeggings(item); break;
        case 0: player.getInventory().setBoots(item); break;
        }

        player.getInventory().clear(slot);

        player.sendMessage("Backpack equipped: " + getPackDisplayName(item));
    }

    /// IDENTIFYING, LOADING, AND SAVING

    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;
    final Enchantment EFFICIENCY = Enchantment.DIG_SPEED;
    final Enchantment PUNCH = Enchantment.ARROW_KNOCKBACK;

    /** Get whether the item is a pack.
    */
    private boolean isPack(ItemStack item) {
        // Represented by a chest with a special enchantment
        return item != null && item.getType() == plugin.chestpackItem && item.containsEnchantment(FORTUNE);
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
        return Math.abs(item.getEnchantmentLevel(EFFICIENCY));
    }

    private boolean hasIntegratedWorkbench(ItemStack item) {
        return item.containsEnchantment(PUNCH);
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
        File packFile = getPackFile(id);
        FileConfiguration packConfig = YamlConfiguration.loadConfiguration(packFile);

        packConfig.set("inventory", inventory.getContents());
        try {
            packConfig.save(packFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Get file for storing given chestpack id. */
    private static File getPackFile(int id) {
        return new File(plugin.getDataFolder(), "pack"+id+".yml"); // TODO: formatting, 0000
    }

    /** Load contents of pack from disk. */
    @SuppressWarnings("unchecked")
    private static void loadPack(int id, Inventory inventory) {

        // try new per-file config
        File packFile = getPackFile(id);
        FileConfiguration packConfig = YamlConfiguration.loadConfiguration(packFile);

        List<?> list = packConfig.getList("inventory");
        if (list != null) {
            for (int i = 0; i < Math.min(list.size(), inventory.getSize()); i += 1) {
                inventory.setItem(i, (ItemStack)list.get(i));
            }
        } else {
            // if fails, try to migrate legacy config
            plugin.reloadConfig();

            list = plugin.getConfig().getList("inventory."+id);
            if (list != null) {
                for (int i = 0; i < Math.min(list.size(), inventory.getSize()); i += 1) {
                    inventory.setItem(i, (ItemStack)list.get(i));
                }
            } 
        }
    }

}

public class Chestpack extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");

    Material chestpackItem;

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();

        String s = getConfig().getString("chestpackItem", "CHEST");
        chestpackItem = Material.matchMaterial(s);
        if (chestpackItem == null) {
            log.warning("Invalid material name "+s+", using chest");
            chestpackItem = Material.CHEST;     // TODO: should we use numeric IDs instead? but then recipes..
        }

        loadRecipes();

        new ChestpackListener(this);
    }

    public void onDisable() {
        Bukkit.resetRecipes();  // TODO: can we reset only ours?
    }

    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;
    final Enchantment EFFICIENCY = Enchantment.DIG_SPEED;
    final Enchantment PUNCH = Enchantment.ARROW_KNOCKBACK;


    private void loadRecipes() {
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

            // can shift-click to open integrated workbench
            // TODO: portable workbench!/project table basically a workbench which doubles as a 3x3 chest 
            // (items persist on grid, don't fall off, like Buildcraft's Automatic Crafting Table / RedPower2 Project Table)
            boolean hasWorkbench = map.get("hasWorkbench") != null && ((Boolean)map.get("hasWorkbench"));

            log.info("Recipe "+material+" x "+count+" = "+size+(hasWorkbench ? " (integrated workbench)" : ""));

            ItemStack emptyPack = new ItemStack(this.chestpackItem, 1);
            emptyPack.addUnsafeEnchantment(FORTUNE, 1);  // blank

            // store size as negative level so hitting blocks with pack doesn't
            // efficiently mine away blocks very quickly
            emptyPack.addUnsafeEnchantment(EFFICIENCY, -size);

            if (hasWorkbench) {
                emptyPack.addUnsafeEnchantment(PUNCH, 1);
            }

            /* // TODO: isolate why shaped recipes still lose on 1.2.3-R0.2
            https://bukkit.atlassian.net/browse/BUKKIT-602
            ShapedRecipe recipe = new ShapedRecipe(emptyPack);
            recipe.shape(
                "LLL",
                "LCL",
                "LLL");
            recipe.setIngredient('L', Material.LEATHER);
            recipe.setIngredient('C', plugin.chestpackItem);
            */

            // as a workaround, do shapeless instead

            ShapelessRecipe recipe = new ShapelessRecipe(emptyPack);
            recipe.addIngredient(count, material, -1);
            recipe.addIngredient(1, this.chestpackItem);

            Bukkit.addRecipe(recipe);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("chestpack")) {
            return false;
        }
    
        if (!(sender instanceof Player)) {
            // TODO: allow listing inventory textually from console
            return false;
        }

        Player player = (Player)sender;

        if (!player.hasPermission("chestpack.admin")) {
            player.sendMessage("You do not have permission");
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        try {
            int id = Integer.parseInt(args[0]);

            int numSlots = getConfig().getInt("maxSlots", 54);

            ChestpackListener.openPack(player, id, numSlots);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
