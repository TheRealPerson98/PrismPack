package com.person98.prismPack.manager;

import com.person98.prismPack.PrismPack;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database management class that handles database connections using HikariCP.
 * Supports both SQLite and MySQL database connections.
 */
public class Database {

    private static HikariDataSource dataSource;
    @Getter
    private static boolean usingSQLite;

    /**
     * Initializes the database connection pool using HikariCP.
     * Configuration is loaded from the plugin's config file.
     * For SQLite, creates a connection to a local database file.
     * For MySQL, creates a connection pool to a remote database server.
     */
    public static void initialize() {
        HikariConfig config = new HikariConfig();
        ConfigManager configManager = ConfigManager.getInstance();

        usingSQLite = configManager.isUsingSQLite();
        
        if (usingSQLite) {
            String dbPath = configManager.getSqlitePath();
            File dataFolder = PrismPack.getInstance().getDataFolder();
            File dbFile = new File(dataFolder, dbPath);
            
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
        } else {
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true",
                    configManager.getMysqlHost(),
                    configManager.getMysqlPort(),
                    configManager.getMysqlDatabase()));
            config.setUsername(configManager.getMysqlUsername());
            config.setPassword(configManager.getMysqlPassword());
            config.setMaximumPoolSize(configManager.getMysqlPoolSize());
        }

        dataSource = new HikariDataSource(config);
    }

    /**
     * Retrieves a connection from the connection pool.
     *
     * @return A database connection from the pool
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Closes the database connection pool.
     * Should be called when the plugin is being disabled.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
