package com.person98.prismPack.manager;

import com.person98.prismPack.PrismPack;
import com.person98.prismPack.util.ItemSerializationUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the storage and retrieval of player backpack inventories in the database.
 * Supports both MySQL and SQLite databases through a unified interface.
 */
public class BackpackManager {

    private static final String CREATE_BACKPACK_PLAYERS_TABLE_MYSQL = "CREATE TABLE IF NOT EXISTS backpack_players (" +
            "player_id INT AUTO_INCREMENT PRIMARY KEY," +
            "uuid CHAR(36) UNIQUE NOT NULL" +
            ")";

    private static final String CREATE_BACKPACK_PLAYERS_TABLE_SQLITE = "CREATE TABLE IF NOT EXISTS backpack_players (" +
            "player_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid CHAR(36) UNIQUE NOT NULL" +
            ")";

    private static final String CREATE_BACKPACKS_TABLE_MYSQL = "CREATE TABLE IF NOT EXISTS backpacks (" +
            "owner INT NOT NULL," +
            "itemstacks LONGTEXT NOT NULL," +
            "version INT DEFAULT 0," +
            "lastupdate DATETIME," +
            "FOREIGN KEY (owner) REFERENCES backpack_players(player_id)" +
            ")";

    private static final String CREATE_BACKPACKS_TABLE_SQLITE = "CREATE TABLE IF NOT EXISTS backpacks (" +
            "owner INTEGER NOT NULL," +
            "itemstacks TEXT NOT NULL," +
            "version INTEGER DEFAULT 0," +
            "lastupdate TIMESTAMP," +
            "FOREIGN KEY (owner) REFERENCES backpack_players(player_id)" +
            ")";

    private static final Map<UUID, Inventory> backpackCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAccessTime = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_TIME = 1000 * 60 * 15; // 15 minutes

    /**
     * Initializes the database tables required for backpack storage.
     * Creates the backpack_players and backpacks tables if they don't exist.
     * Automatically detects and uses the appropriate SQL syntax based on the database type.
     */
    public static void initialize() {
        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            boolean usingSQLite = Database.isUsingSQLite();
            
            statement.executeUpdate(usingSQLite ? 
                CREATE_BACKPACK_PLAYERS_TABLE_SQLITE : 
                CREATE_BACKPACK_PLAYERS_TABLE_MYSQL);
                
            statement.executeUpdate(usingSQLite ? 
                CREATE_BACKPACKS_TABLE_SQLITE : 
                CREATE_BACKPACKS_TABLE_MYSQL);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves a player's backpack inventory to the database asynchronously.
     *
     * @param playerUUID The UUID of the player whose inventory is being saved
     * @param inventory The inventory contents to save
     */
    public static void saveInventory(UUID playerUUID, Inventory inventory) {
        backpackCache.put(playerUUID, inventory);
        lastAccessTime.put(playerUUID, System.currentTimeMillis());

        Bukkit.getScheduler().runTaskAsynchronously(PrismPack.getInstance(), () -> {
            try (Connection connection = Database.getConnection()) {
                int playerId = getPlayerId(connection, playerUUID);
                if (playerId == -1) {
                    String insertPlayerSQL = "INSERT INTO backpack_players (uuid) VALUES (?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertPlayerSQL, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, playerUUID.toString());
                        ps.executeUpdate();

                        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                playerId = generatedKeys.getInt(1);
                            }
                        }
                    }
                }

                ItemStack[] items = inventory.getContents();
                String serializedInventory = ItemSerializationUtil.serializeInventory(items);

                String upsertSQL = Database.isUsingSQLite() ?
                        "INSERT OR REPLACE INTO backpacks (owner, itemstacks, lastupdate) VALUES (?, ?, ?)" :
                        "INSERT INTO backpacks (owner, itemstacks, lastupdate) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE itemstacks = VALUES(itemstacks), lastupdate = VALUES(lastupdate)";

                try (PreparedStatement ps = connection.prepareStatement(upsertSQL)) {
                    ps.setInt(1, playerId);
                    ps.setString(2, serializedInventory);
                    ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Loads a player's backpack inventory, first checking cache then database.
     *
     * @param playerUUID The UUID of the player whose inventory should be loaded
     * @return The loaded inventory, or null if no inventory exists for the player
     */
    public static Inventory loadInventory(UUID playerUUID) {
        Inventory cachedInventory = backpackCache.get(playerUUID);
        if (cachedInventory != null) {
            Long lastAccess = lastAccessTime.get(playerUUID);
            if (lastAccess != null && System.currentTimeMillis() - lastAccess < CACHE_EXPIRY_TIME) {
                lastAccessTime.put(playerUUID, System.currentTimeMillis());
                return cachedInventory;
            }
        }

        try (Connection connection = Database.getConnection()) {
            int playerId = getPlayerId(connection, playerUUID);
            if (playerId == -1) return null;

            String selectBackpackSQL = "SELECT itemstacks FROM backpacks WHERE owner = ?";
            try (PreparedStatement ps = connection.prepareStatement(selectBackpackSQL)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String serializedInventory = rs.getString("itemstacks");
                        ItemStack[] items = ItemSerializationUtil.deserializeInventory(serializedInventory);
                        Inventory inventory = Bukkit.createInventory(null, items.length);
                        inventory.setContents(items);
                        
                        backpackCache.put(playerUUID, inventory);
                        lastAccessTime.put(playerUUID, System.currentTimeMillis());
                        
                        return inventory;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cleans up expired cache entries. Should be called periodically.
     */
    public static void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        lastAccessTime.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > CACHE_EXPIRY_TIME) {
                backpackCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Helper method to retrieve a player's ID from the database.
     *
     * @param connection The active database connection
     * @param playerUUID The UUID of the player to look up
     * @return The player's ID, or -1 if the player doesn't exist
     * @throws SQLException if a database error occurs
     */
    private static int getPlayerId(Connection connection, UUID playerUUID) throws SQLException {
        String selectPlayerSQL = "SELECT player_id FROM backpack_players WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectPlayerSQL)) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player_id");
                }
            }
        }
        return -1;
    }
}
