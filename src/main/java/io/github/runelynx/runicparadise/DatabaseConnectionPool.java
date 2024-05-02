package io.github.runelynx.runicparadise;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionPool {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    private static Plugin instance = RunicParadise.getInstance();

    static FileConfiguration fileConfig = instance.getConfig();
	static String dbUsernameFromConfig = fileConfig.getString("dbUser");
    static String dbPasswordFromConfig = fileConfig.getString("dbPassword");

    static {
        config.setJdbcUrl("jdbc:mysql://localhost:3306/rpgame?useSSL=true&verifyServerCertificate=false");
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