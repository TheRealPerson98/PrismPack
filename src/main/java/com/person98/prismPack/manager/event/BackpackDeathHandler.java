package com.person98.prismPack.manager.event;


import com.person98.prismPack.PrismPack;
import com.person98.prismPack.manager.BackpackManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the behavior of backpack contents when a player dies.
 * This class manages whether backpack items are kept or dropped based on
 * game rules and permissions.
 */
public class BackpackDeathHandler implements Listener {

    private final PrismPack plugin = PrismPack.getInstance();

    /**
     * Initializes the death handler and registers event listeners.
     */
    public BackpackDeathHandler() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handles player death events and manages backpack contents.
     * Items are kept if:
     * - The world has keepInventory enabled
     * - The player has the 'backpack.keepOnDeath' permission
     * Otherwise, items are dropped at the player's death location.
     *
     * @param event The PlayerDeathEvent
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check if the world has KeepInventory enabled
        boolean keepInventory = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY) != null &&
                player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);

        // Check if the player has the permission to keep their backpack on death
        if (keepInventory || player.hasPermission("backpack.keepOnDeath")) {
            return;
        }

        // Otherwise, drop the backpack items
        Inventory backpack = BackpackManager.loadInventory(player.getUniqueId());
        if (backpack != null) {
            for (ItemStack item : backpack.getContents()) {
                if (item != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item); // Drop items at player's location
                }
            }

            // Optionally, clear the backpack inventory after death
            backpack.clear();
            BackpackManager.saveInventory(player.getUniqueId(), backpack); // Save the emptied backpack
        }
    }
}
