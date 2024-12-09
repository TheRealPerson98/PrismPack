package com.person98.prismPack.manager;

import com.person98.prismPack.PrismPack;
import com.person98.prismPack.util.PLogger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages configuration settings for the PrismPack plugin.
 * This singleton class handles all configuration values including GUI settings,
 * sound effects, database connections, and blocked items.
 */
@Getter
public class ConfigManager {
    private static ConfigManager instance;
    
    private final String backpackTitle;
    private final Sound openSound;
    private final float openSoundVolume;
    private final float openSoundPitch;
    private final Sound closeSound;
    private final float closeSoundVolume;
    private final float closeSoundPitch;
    private final int cooldown;
    private final Set<Material> blockedItems;
    private final boolean usingSQLite;
    private final String sqlitePath;
    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final int mysqlPoolSize;

    /**
     * Private constructor that initializes all configuration values from the config file.
     * This should only be called through the initialize() method.
     */
    private ConfigManager() {
        PrismPack plugin = PrismPack.getInstance();
        
        // Database settings
        this.usingSQLite = plugin.getConfig().getBoolean("sqlite.enabled", false);
        this.sqlitePath = plugin.getConfig().getString("sqlite.path", "database.db");
        this.mysqlHost = plugin.getConfig().getString("mysql.host", "localhost");
        this.mysqlPort = plugin.getConfig().getInt("mysql.port", 3306);
        this.mysqlDatabase = plugin.getConfig().getString("mysql.database", "database");
        this.mysqlUsername = plugin.getConfig().getString("mysql.username", "root");
        this.mysqlPassword = plugin.getConfig().getString("mysql.password", "password");
        this.mysqlPoolSize = plugin.getConfig().getInt("mysql.poolsize", 10);

        // GUI settings
        ConfigurationSection guiSection = plugin.getConfig().getConfigurationSection("BackPackGUI");
        if(guiSection == null) {
            PLogger.severe("BackPackGUI section not found in config file.");
        }
        this.backpackTitle = guiSection.getString("name", "%player%'s Backpack");// Keep placeholder for later replacement

        // Sound settings
        ConfigurationSection soundSection = guiSection.getConfigurationSection("sound");
        if(soundSection == null) {
            PLogger.severe("sound section not found in config file.");
        }
        this.openSound = Sound.valueOf(soundSection.getString("name", "ENTITY_SHULKER_OPEN"));
        this.openSoundVolume = (float) soundSection.getDouble("volume", 1.0);
        this.openSoundPitch = (float) soundSection.getDouble("pitch", 1.0);

        ConfigurationSection closeSoundSection = guiSection.getConfigurationSection("close_sound");
        if(closeSoundSection == null) {
            PLogger.severe("close_sound section not found in config file.");
        }
        this.closeSound = Sound.valueOf(closeSoundSection.getString("name", "ENTITY_SHULKER_CLOSE"));
        this.closeSoundVolume = (float) closeSoundSection.getDouble("volume", 1.0);
        this.closeSoundPitch = (float) closeSoundSection.getDouble("pitch", 1.0);

        // Cooldown
        this.cooldown = guiSection.getInt("cooldown", 30);

        // Blocked items
        this.blockedItems = new HashSet<>();
        List<String> blockedItemsList = guiSection.getStringList("blocked-items");
        for (String item : blockedItemsList) {
            try {
                this.blockedItems.add(Material.valueOf(item.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in blocked-items: " + item);
            }
        }
    }

    /**
     * Initializes the ConfigManager singleton instance.
     * This must be called before getInstance() can be used.
     */
    public static void initialize() {
        if (instance == null) {
            instance = new ConfigManager();
        }
    }

    /**
     * Returns the singleton instance of ConfigManager.
     *
     * @return The ConfigManager instance
     * @throws IllegalStateException if the ConfigManager hasn't been initialized
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager has not been initialized!");
        }
        return instance;
    }

    /**
     * Reloads the configuration from disk and recreates the ConfigManager instance.
     * This should be called when the config file has been modified and needs to be reloaded.
     */
    public static void reload() {
        PrismPack.getInstance().reloadConfig();
        instance = new ConfigManager();
    }
} 