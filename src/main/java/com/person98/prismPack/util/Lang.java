package com.person98.prismPack.util;


import com.person98.prismPack.PrismPack;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/**
 * Handles language configuration and message management for the plugin.
 * Loads and manages messages from lang.yml configuration file.
 */
public class Lang {

    private static File langFile;
    @Getter
    private static FileConfiguration langConfig;

    /**
     * Sets up the language system by creating and loading the lang.yml file.
     * If the file doesn't exist, it will be created from the plugin's resources.
     */
    public static void setup() {
        // Get the plugin instance
        PrismPack plugin = PrismPack.getInstance();

        // Create the lang.yml file in the plugin folder if it doesn't exist
        langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false); // Copies lang.yml from the plugin jar to the plugin folder
        }

        // Load the lang.yml configuration
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * Enumeration of all available language messages with their configuration paths.
     */
    @Getter
    public enum LANG {
        ONCOOLDOWN("messages.onCooldown"),
        OPENED_BACKPACK("messages.openedBackpack"),
        NO_BACKPACK("messages.noBackpack"),
        CLEANED_BACKPACK("messages.cleanedBackpack"),
        CLEANED_OTHERS_BACKPACK("messages.cleanedOthersBackpack"),
        PLAYER_NOT_FOUND("messages.playerNotFound"),
        OPENED_BACKPACK_OTHER("messages.openedBackpackOther"),
        RELOADED_CONFIG("messages.reloadedConfig"),
        BLACKLISTED_ITEM("messages.blacklistedItem");

        private final String path;

        /**
         * @param path The configuration path for this message
         */
        LANG(String path) {
            this.path = path;
        }

    }

    /**
     * Sends a configured message to a player with optional placeholder replacements.
     * Messages support MiniMessage formatting.
     *
     * @param player The player to send the message to
     * @param messageType The type of message to send from the LANG enum
     * @param placeholders Optional placeholders in pairs (placeholder, value)
     */
    public static void sendMessage(Player player, LANG messageType, String... placeholders) {
        String message = langConfig.getString(messageType.getPath(), "<red>Message not found.");
        // Replace placeholders if provided
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    /**
     * Saves the current language configuration to file.
     * Any runtime changes to the configuration will be persisted.
     */
    public static void saveLang() {
        try {
            langConfig.save(langFile);
        } catch (IOException e) {
            PLogger.severe("Failed to save lang.yml: " + e);
        }
    }

    /**
     * Reloads the language configuration from file.
     * Any changes made to lang.yml will be loaded into memory.
     */
    public static void reloadLang() {
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
}
