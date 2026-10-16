package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.service.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB connection settings come from AppConfig (/etc/mailadmin/config.properties). No fallback
 * defaults on purpose — if these are blank, SetupGateFilter should already have routed every
 * request to /setup before any DAO ever calls getConnection(). The JDBC URL's scheme selects
 * which driver handles the connection; we register every driver that happens to be on the
 * classpath (MariaDB is always bundled, PostgreSQL only if its jar was installed — see the
 * install steps / DbSystem).
 */
public final class Database {

    static {
        loadDriverIfPresent("org.mariadb.jdbc.Driver");  // also serves MySQL (jdbc:mariadb://)
        loadDriverIfPresent("org.postgresql.Driver");
    }

    private static void loadDriverIfPresent(String driverClass) {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException ignored) {
            // Optional driver not bundled in this deployment — connections using its URL scheme
            // will fail with a clear "No suitable driver" later, which is the right signal.
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
