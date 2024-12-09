package com.person98.prismPack.manager.ui;

import com.person98.prismPack.PrismPack;
import com.person98.prismPack.manager.BackpackManager;
import com.person98.prismPack.manager.ConfigManager;
import com.person98.prismPack.util.Lang;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages the administrative interface for viewing and modifying player backpacks.
 * This class handles the GUI interaction when administrators inspect other players' backpacks.
 */
public class AdminBackpack implements Listener {

    private final PrismPack plugin = PrismPack.getInstance();
    private final Map<UUID, UUID> adminToTargetMap = new HashMap<>(); // Map admin UUID to target player's UUID
    private final Map<UUID, Inventory> openBackpacks = new HashMap<>(); // Track each opened backpack by target player
    private final Set<Material> blockedItems; // List of blocked items from config
    private final ConfigManager config;

    /**
     * Initializes the AdminBackpack system and registers event listeners.
     * Loads blocked items from the configuration.
     */
    public AdminBackpack() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.config = ConfigManager.getInstance();
        this.blockedItems = config.getBlockedItems();
    }

    /**
     * Opens a player's backpack for administrative inspection.
     * 
     * @param admin The administrator viewing the backpack
     * @param targetPlayer The player whose backpack is being viewed (can be offline)
     */
    public void openBackpackForAdmin(Player admin, OfflinePlayer targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();
        String guiTitle = getGuiTitle(targetPlayer);

        // Load the target player's inventory from storage or initialize a new one
        Inventory storedInventory = BackpackManager.loadInventory(targetUUID);
        if (storedInventory == null) {
            Lang.sendMessage(admin, Lang.LANG.NO_BACKPACK, "%player%", targetPlayer.getName());
            return;
        }

        Inventory backpack = Bukkit.createInventory(admin, storedInventory.getSize(), MiniMessage.miniMessage().deserialize(guiTitle));

        // Set contents from the stored inventory
        backpack.setContents(storedInventory.getContents());

        // Map the admin to the target player and track the backpack inventory
        adminToTargetMap.put(admin.getUniqueId(), targetUUID);
        openBackpacks.put(targetUUID, backpack);

        // Open the inventory for the admin
        admin.openInventory(backpack);

        // Play the opening sound
        playOpeningSound(admin);
    }

    /**
     * Handles the closing of backpack inventories.
     * Saves changes if the admin has edit permissions and cleans up tracking maps.
     * 
     * @param event The InventoryCloseEvent
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID adminUUID = player.getUniqueId();

        // Check if the admin was viewing a target player's backpack
        if (adminToTargetMap.containsKey(adminUUID)) {
            UUID targetUUID = adminToTargetMap.get(adminUUID);
            Inventory closedInventory = openBackpacks.get(targetUUID);

            // Save the target player's backpack only if the admin has the permission to edit
            if (player.hasPermission("backpack.others.edit")) {
                BackpackManager.saveInventory(targetUUID, closedInventory);
            }

            // Clean up after closing
            adminToTargetMap.remove(adminUUID);
            openBackpacks.remove(targetUUID);
            playClosingSound(player);
        }
    }

    /**
     * Handles clicks within backpack inventories.
     * Prevents modifications if the admin lacks edit permissions.
     * 
     * @param event The InventoryClickEvent
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID adminUUID = player.getUniqueId();

        // Check if the admin is viewing another player's backpack
        if (adminToTargetMap.containsKey(adminUUID)) {
            if (!player.hasPermission("backpack.others.edit")) {
                event.setCancelled(true); // Block the click
            }
        }
    }

    /**
     * Checks if an item is in the blocked items list.
     * 
     * @param item The ItemStack to check
     * @return true if the item is blocked, false otherwise
     */
    private boolean isBlockedItem(ItemStack item) {
        return blockedItems.contains(item.getType());
    }

    /**
     * Generates the GUI title for a player's backpack.
     * 
     * @param player The player whose backpack is being viewed
     * @return The formatted GUI title string
     */
    private String getGuiTitle(OfflinePlayer player) {
        return config.getBackpackTitle().replace("%player%", Objects.requireNonNull(player.getName()));
    }

    /**
     * Plays the backpack opening sound for a player.
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
     * Plays the backpack closing sound for a player.
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
