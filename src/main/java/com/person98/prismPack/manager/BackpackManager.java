package com.person98.prismPack.manager;

import com.person98.prismPack.util.ItemSerializationUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.UUID;

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
     * Saves a player's backpack inventory to the database.
     * Creates a new player entry if the UUID doesn't exist.
     *
     * @param playerUUID The UUID of the player whose inventory is being saved
     * @param inventory The inventory contents to save
     */
    public static void saveInventory(UUID playerUUID, Inventory inventory) {
        try (Connection connection = Database.getConnection()) {

            // Check if player exists in the backpack_players table
            int playerId = getPlayerId(connection, playerUUID);

            if (playerId == -1) {
                // Insert new player if not found
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

            // Serialize the inventory
            ItemStack[] items = inventory.getContents();
            String serializedInventory = ItemSerializationUtil.serializeInventory(items);

            // Insert or update the backpack
            String selectBackpackSQL = "SELECT owner FROM backpacks WHERE owner = ?";
            try (PreparedStatement ps = connection.prepareStatement(selectBackpackSQL)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Update existing backpack
                        String updateBackpackSQL = "UPDATE backpacks SET itemstacks = ?, lastupdate = ? WHERE owner = ?";
                        try (PreparedStatement updatePs = connection.prepareStatement(updateBackpackSQL)) {
                            updatePs.setString(1, serializedInventory);
                            updatePs.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                            updatePs.setInt(3, playerId);
                            updatePs.executeUpdate();
                        }
                    } else {
                        // Insert new backpack
                        String insertBackpackSQL = "INSERT INTO backpacks (owner, itemstacks, lastupdate) VALUES (?, ?, ?)";
                        try (PreparedStatement insertPs = connection.prepareStatement(insertBackpackSQL)) {
                            insertPs.setInt(1, playerId);
                            insertPs.setString(2, serializedInventory);
                            insertPs.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                            insertPs.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a player's backpack inventory from the database.
     *
     * @param playerUUID The UUID of the player whose inventory should be loaded
     * @return The loaded inventory, or null if no inventory exists for the player
     */
    public static Inventory loadInventory(UUID playerUUID) {
        try (Connection connection = Database.getConnection()) {

            int playerId = getPlayerId(connection, playerUUID);

            if (playerId == -1) {
                // Player not found
                return null;
            }

            String selectBackpackSQL = "SELECT itemstacks FROM backpacks WHERE owner = ?";
            try (PreparedStatement ps = connection.prepareStatement(selectBackpackSQL)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String serializedInventory = rs.getString("itemstacks");
                        ItemStack[] items = ItemSerializationUtil.deserializeInventory(serializedInventory);
                        Inventory inventory = Bukkit.createInventory(null, items.length);
                        inventory.setContents(items);
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
