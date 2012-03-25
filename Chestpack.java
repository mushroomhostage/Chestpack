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
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onInventoryClose(InventoryCloseEvent event) {
        for (HumanEntity viewer: event.getViewers()) {
            if (!(viewer instanceof Player)) {
                continue;
            }

            // crafted a backpack? wear it!
            checkEquipPack((Player)viewer);
        }
    }

    // TODO: why broken?
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        // picked up a backpack item? (TODO: prevent?)
        checkEquipPack(event.getPlayer());
    }

    /** Wear a player's pack in their chestplate slot, if they have one in their inventory.
    */
    private void checkEquipPack(Player player) {
        ItemStack[] inventory = player.getInventory().getContents();

        for (int i = 0; i < inventory.length; i += 1) {
            ItemStack item = inventory[i];
            if (isPack(item)) {
                // existing armor falls off
                ItemStack drop = player.getInventory().getChestplate();
                if (drop != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }

                // equip pack
                player.getInventory().setChestplate(item);
                player.getInventory().clear(i);

                player.sendMessage("Backpack equipped");
            }
        }
    }

    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;

    /** Get whether the item is a chestpack.
    */
    private boolean isPack(ItemStack item) {
        // Represented by a chest with a special enchantment
        return item != null && item.getType() == Material.CHEST && item.containsEnchantment(FORTUNE);
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item itemEntity = event.getItemDrop();
        ItemStack itemStack = itemEntity.getItemStack();

        if (!isPack(itemStack)) {
            return;
        }

        Player player = event.getPlayer();

        itemEntity.remove();

        player.sendMessage("Backpack dropped");

        // Find out where to drop backpack as a block
        Block block = player.getTargetBlock(null, 5).getRelative(BlockFace.UP);
        // TODO: get face of targetted block
        // TODO: only place in air
        if (block == null) {
            // TODO: set in a more reasonable location
            block = itemEntity.getLocation().getBlock();
        }


        // TODO: permissions
        block.setTypeIdAndData(Material.CHEST.getId(), (byte)0, true);
        // TODO: identify as pack
        // TODO: populate contents
    }
}

public class Chestpack extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");

    public void onEnable() {
        loadRecipe();

        new ChestpackListener(this);
    }

    public void onDisable() {
        Bukkit.resetRecipes();  // TODO: can we reset only ours?
    }

    final Enchantment FORTUNE = Enchantment.LOOT_BONUS_BLOCKS;

    private void loadRecipe() {
        ItemStack emptyPack = new ItemStack(Material.CHEST, 1);
        emptyPack.addUnsafeEnchantment(FORTUNE, 1);

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
        recipe.addIngredient(8, Material.LEATHER);
        recipe.addIngredient(1, Material.CHEST);

        Bukkit.addRecipe(recipe);
    }
}
