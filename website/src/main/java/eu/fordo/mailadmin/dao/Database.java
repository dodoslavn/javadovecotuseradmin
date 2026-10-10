package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.service.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB connection settings come from AppConfig (/etc/mailadmin/config.properties). No fallback
 * defaults on purpose — if these are blank, SetupGateFilter should already have routed every
 * request to /setup before any DAO ever calls getConnection().
 */
public final class Database {

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB JDBC driver not on classpath", e);
        }
    }

    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = AppConfig.get("db.url", "");
        String user = AppConfig.get("db.user", "");
        String password = AppConfig.get("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }
}
