package io.github.runelynx.runicparadise;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.bukkit.Bukkit.getServer;

/**
 * Shared MySQL connection pool. Since 2026-11-23 MySQL.openConnection() hands out pooled connections, so the
 * ~100 call sites reuse open connections instead of doing a fresh TCP+TLS login each time (several per join).
 * Callers still call close(), which returns the connection to the pool.
 */
public class DatabaseConnectionPool {
    private static HikariDataSource ds;

    private DatabaseConnectionPool() {}

    private static synchronized HikariDataSource dataSource() {
        if (ds == null || ds.isClosed()) {
            File dbFile = new File(getServer().getPluginManager().getPlugin("RunicParadise").getDataFolder(), "db-details.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(dbFile);

            HikariConfig config = new HikariConfig();
            config.setPoolName("RunicParadise-DB");
            config.setJdbcUrl("jdbc:mysql://" + cfg.getString("dbHost") + ":" + cfg.getString("dbPort") + "/"
                    + cfg.getString("dbDatabase") + "?useSSL=true&verifyServerCertificate=false");
            config.setUsername(cfg.getString("dbUser"));
            config.setPassword(cfg.getString("dbPassword"));
            config.setMaximumPoolSize(16);
            config.setMinimumIdle(2);
            // Never block the main thread for long: if the pool is exhausted, MySQL.openConnection() falls back
            // to a direct connection after this wait.
            config.setConnectionTimeout(2000);
            // Log (with a stack trace) any connection held longer than 20 s, i.e. code that forgot to close it.
            config.setLeakDetectionThreshold(20000);
            config.setMaxLifetime(1800000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            ds = new HikariDataSource(config);
        }
        return ds;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    /** Called from onDisable so reloads/restarts don't leave connections open. */
    public static synchronized void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}
