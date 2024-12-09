package com.person98.prismPack.manager.ui;

import com.person98.prismPack.PrismPack;
import com.person98.prismPack.manager.BackpackManager;
import com.person98.prismPack.manager.ConfigManager;
import com.person98.prismPack.util.Lang;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the backpack GUI interface and handles all inventory-related events.
 * This class is responsible for creating, displaying, and managing player backpack inventories,
 * including item restrictions and permission-based size control.
 */
public class Backpack implements Listener {

    private final PrismPack plugin = PrismPack.getInstance();
    private final Map<UUID, Inventory> openBackpacks = new HashMap<>(); // Track each player's opened backpack
    private final Set<Material> blockedItems; // List of blocked items from config
    private final ConfigManager config;

    /**
     * Initializes the backpack system and registers event listeners.
     * Loads blocked items from the configuration.
     */
    public Backpack() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.config = ConfigManager.getInstance();
        this.blockedItems = config.getBlockedItems();
    }

    /**
     * Opens a backpack inventory for a player.
     * The size of the backpack is determined by the player's permissions.
     * Loads existing contents if the player has previously saved items.
     *
     * @param player The player to open the backpack for
     */
    public void openBackpack(Player player) {
        int size = getBackpackSize(player);
        String guiTitle = getGuiTitle(player);

        Inventory backpack = Bukkit.createInventory(player, size, MiniMessage.miniMessage().deserialize(guiTitle));

        // Load the inventory from storage or initialize a new one
        Inventory storedInventory = BackpackManager.loadInventory(player.getUniqueId());
        if (storedInventory != null) {
            backpack.setContents(storedInventory.getContents());
        }

        openBackpacks.put(player.getUniqueId(), backpack); // Store the opened backpack in the map
        player.openInventory(backpack);

        // Play the opening sound
        playOpeningSound(player);
    }

    /**
     * Handles clicks within the backpack inventory.
     * Prevents players from storing blocked items and handles hotbar key transfers.
     *
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID playerUUID = player.getUniqueId();
        if (openBackpacks.containsKey(playerUUID)) {
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            int hotbarButton = event.getHotbarButton();

            // Check if the player is interacting with their backpack
            if ((clickedItem != null && isBlockedItem(clickedItem)) ||
                    (cursorItem != null && isBlockedItem(cursorItem))) {
                event.setCancelled(true);
                Lang.sendMessage(player, Lang.LANG.BLACKLISTED_ITEM);
                return;
            }

            // Detect if the player is using number keys to move items from hotbar
            if (hotbarButton >= 0 && hotbarButton <= 8) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && isBlockedItem(hotbarItem)) {
                    event.setCancelled(true);
                    Lang.sendMessage(player, Lang.LANG.BLACKLISTED_ITEM);
                }
            }
        }
    }

    /**
     * Handles the closing of backpack inventories.
     * Saves the contents and plays closing sound effects.
     *
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID playerUUID = player.getUniqueId();

        // Check if the closed inventory is a tracked backpack
        if (openBackpacks.containsKey(playerUUID)) {
            Inventory closedInventory = openBackpacks.get(playerUUID);

            // Save the backpack on close
            BackpackManager.saveInventory(playerUUID, closedInventory);

            // Remove the backpack from the map after saving
            openBackpacks.remove(playerUUID);
            playClosingSound(player);
        }
    }

    /**
     * Checks if an item is in the blocked items list.
     *
     * @param item The item to check
     * @return true if the item is blocked, false otherwise
     */
    private boolean isBlockedItem(ItemStack item) {
        return blockedItems.contains(item.getType());
    }

    /**
     * Determines the size of a player's backpack based on their permissions.
     * Permissions follow the format: backpack.size.X where X is 2-6
     *
     * @param player The player to check permissions for
     * @return The size of the inventory (multiples of 9 from 9 to 54)
     */
    private int getBackpackSize(Player player) {
        if (player.hasPermission("backpack.size.6")) return 54;
        if (player.hasPermission("backpack.size.5")) return 45;
        if (player.hasPermission("backpack.size.4")) return 36;
        if (player.hasPermission("backpack.size.3")) return 27;
        if (player.hasPermission("backpack.size.2")) return 18;
        return 9; // Default size
    }

    /**
     * Gets the customized GUI title for a player's backpack.
     * Replaces the %player% placeholder with the player's name.
     *
     * @param player The player to get the title for
     * @return The formatted GUI title
     */
    private String getGuiTitle(Player player) {
        return config.getBackpackTitle().replace("%player%", player.getName());
    }

    /**
     * Plays the backpack opening sound effect for a player.
     * Sound settings are loaded from the configuration.
     *
     * @param player The player to play the sound for
     */
    private void playOpeningSound(Player player) {
        player.playSound(player.getLocation(), 
            config.getOpenSound(), 
            config.getOpenSoundVolume(), 
            config.getOpenSoundPitch());
    }

    /**
     * Plays the backpack closing sound effect for a player.
     * Sound settings are loaded from the configuration.
     *
     * @param player The player to play the sound for
     */
    private void playClosingSound(Player player) {
        player.playSound(player.getLocation(), 
            config.getCloseSound(), 
            config.getCloseSoundVolume(), 
            config.getCloseSoundPitch());
    }
}
