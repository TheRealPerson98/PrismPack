package com.person98.prismPack.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;

import com.person98.prismPack.PrismPack;
import com.person98.prismPack.manager.BackpackManager;
import com.person98.prismPack.manager.ConfigManager;
import com.person98.prismPack.manager.ui.AdminBackpack;
import com.person98.prismPack.manager.ui.Backpack;
import com.person98.prismPack.util.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CommandAlias("prismpack|backpack|bp")
public class BackPackCommand extends BaseCommand {

    private final Backpack backpack = new Backpack();
    private final AdminBackpack adminBackpack = new AdminBackpack();
    private final Map<UUID, Long> lastOpenedTime = new HashMap<>(); // Track last open time for players

    @Subcommand("reload")
    @CommandPermission("prismpack.command.reload")
    public void onReload(Player player) {

        PrismPack.getInstance().reload();

        Lang.sendMessage(player, Lang.LANG.RELOADED_CONFIG);
    }

    @Subcommand("clean")
    @CommandPermission("backpack.clean")
    public void onClean(Player player) {
        // Clean the player's own backpack
        Inventory inventory = BackpackManager.loadInventory(player.getUniqueId());
        if (inventory != null) {
            inventory.clear();
            BackpackManager.saveInventory(player.getUniqueId(), inventory);
            Lang.sendMessage(player, Lang.LANG.CLEANED_BACKPACK);
        } else {
            Lang.sendMessage(player, Lang.LANG.NO_BACKPACK);
        }
    }

    @Subcommand("clean")
    @CommandPermission("backpack.clean.others")
    public void onCleanOther(Player player, String targetName) {
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer != null) {
            Inventory inventory = BackpackManager.loadInventory(targetPlayer.getUniqueId());
            if (inventory != null) {
                inventory.clear();
                BackpackManager.saveInventory(targetPlayer.getUniqueId(), inventory);
                Lang.sendMessage(player, Lang.LANG.CLEANED_OTHERS_BACKPACK, "%player%", targetPlayer.getName());
            } else {
                Lang.sendMessage(player, Lang.LANG.NO_BACKPACK);
            }
        } else {
            Lang.sendMessage(player, Lang.LANG.PLAYER_NOT_FOUND);
        }
    }

    @Subcommand("open")
    @CommandPermission("backpack.open.others")
    public void onOpenOther(Player admin, String targetName) {
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (targetPlayer != null) {
            adminBackpack.openBackpackForAdmin(admin, targetPlayer);
            Lang.sendMessage(admin, Lang.LANG.OPENED_BACKPACK_OTHER, "%player%", targetPlayer.getName());
        } else {
            Lang.sendMessage(admin, Lang.LANG.PLAYER_NOT_FOUND);
        }
    }

    @Default
    @CommandPermission("backpack.use")
    public void onDefault(Player player) {
        if (isOnCooldown(player)) {
            long timeLeft = getCooldownTimeLeft(player);
            Lang.sendMessage(player, Lang.LANG.ONCOOLDOWN, "%time_left%", String.valueOf(timeLeft));
            return;
        }

        // Open the backpack and update the last opened time
        backpack.openBackpack(player);
        lastOpenedTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isOnCooldown(Player player) {
        if (player.hasPermission("backpack.noCooldown")) {
            return false; // Bypass cooldown if player has permission
        }

        int cooldownSeconds = ConfigManager.getInstance().getCooldown();
        UUID playerUUID = player.getUniqueId();
        if (!lastOpenedTime.containsKey(playerUUID)) {
            return false; // First time opening the backpack
        }

        long lastOpened = lastOpenedTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastOpened) < (cooldownSeconds * 1000L); // Convert seconds to milliseconds
    }

    private long getCooldownTimeLeft(Player player) {
        int cooldownSeconds = ConfigManager.getInstance().getCooldown();
        long lastOpened = lastOpenedTime.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        return (cooldownSeconds - ((currentTime - lastOpened) / 1000)); // Return time left in seconds
    }
}
