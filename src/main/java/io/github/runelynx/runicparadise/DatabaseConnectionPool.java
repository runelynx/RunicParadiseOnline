package io.github.runelynx.runicparadise;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.bukkit.Bukkit.getServer;

public class DatabaseConnectionPool {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static File dbFile = new File(
            getServer().getPluginManager().getPlugin("RunicParadise").getDataFolder().getAbsolutePath(),
            "db-details.yml");

    static FileConfiguration dbFileConfig = YamlConfiguration.loadConfiguration(dbFile);

	static String dbUsernameFromConfig = dbFileConfig.getString("dbUser");
    static String dbPasswordFromConfig = dbFileConfig.getString("dbPassword");
    static String dbHostFromConfig = dbFileConfig.getString("dbHost");
    static String dbPortFromConfig = dbFileConfig.getString("dbPort");
    static String dbDatabaseFromConfig = dbFileConfig.getString("dbDatabase");

    static {
        config.setJdbcUrl("jdbc:mysql://"+ dbHostFromConfig +":"+ dbPortFromConfig +"/"+ dbDatabaseFromConfig +"?useSSL=true&verifyServerCertificate=false");
        config.setUsername(dbUsernameFromConfig);
        config.setPassword(dbPasswordFromConfig);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    private DatabaseConnectionPool() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}