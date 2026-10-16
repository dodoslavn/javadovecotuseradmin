package eu.fordo.mailadmin.service;

/**
 * The database systems this app can run against, and everything that differs between them:
 * JDBC driver class, URL scheme, default port, the host commands shown in the setup wizard, and
 * which SQL dialect their DDL needs (see SchemaInstaller).
 *
 * MySQL deliberately reuses the MariaDB Connector/J driver (jdbc:mariadb://): there is no MySQL
 * Connector/J in apt, and the MariaDB driver speaks the MySQL wire protocol fine. MySQL and
 * MariaDB also take byte-for-byte identical DDL, so they share a dialect — only PostgreSQL
 * differs (no inline ENUM, IDENTITY instead of AUTO_INCREMENT, no ON UPDATE clause, etc.).
 * Oracle/DB2 are intentionally unsupported: no apt-packaged JDBC driver exists for either.
 */
public enum DbSystem {
    MARIADB("mariadb", "MariaDB", "org.mariadb.jdbc.Driver", "mariadb", 3306),
    MYSQL("mysql", "MySQL", "org.mariadb.jdbc.Driver", "mariadb", 3306),
    POSTGRESQL("postgresql", "PostgreSQL", "org.postgresql.Driver", "postgresql", 5432);

    private final String code;
    private final String displayName;
    private final String driverClass;
    private final String urlScheme;
    private final int defaultPort;

    DbSystem(String code, String displayName, String driverClass, String urlScheme, int defaultPort) {
        this.code = code;
        this.displayName = displayName;
        this.driverClass = driverClass;
        this.urlScheme = urlScheme;
        this.defaultPort = defaultPort;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }
    public String driverClass() { return driverClass; }
    public int defaultPort() { return defaultPort; }
    public boolean isPostgres() { return this == POSTGRESQL; }

    public String jdbcUrl(String host, String port, String dbName) {
        return "jdbc:" + urlScheme + "://" + host + ":" + port + "/" + dbName;
    }

    public static DbSystem fromCode(String code) {
        if (code != null) {
            for (DbSystem s : values()) {
                if (s.code.equalsIgnoreCase(code.trim())) {
                    return s;
                }
            }
        }
        return MARIADB; // safe default; also what pre-multi-DB config files implied
    }

    /**
     * Best-effort dialect check from a JDBC URL: true for PostgreSQL, false for the
     * MySQL/MariaDB family. Used by SchemaInstaller when all it has is a live connection.
     * Can't (and needn't) tell MariaDB from MySQL — they take identical DDL.
     */
    public static boolean urlIsPostgres(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql:");
    }

    /** Human-friendly system name for a stored db.url (for the read-only server-config display). */
    public static String displayNameForUrl(String jdbcUrl) {
        return urlIsPostgres(jdbcUrl) ? POSTGRESQL.displayName : MARIADB.displayName + " / " + MYSQL.displayName;
    }

    /**
     * The host-side commands the setup wizard tells the admin to run to create the database and a
     * login for the app. Shared leading line creates the config dir; the rest is system-specific.
     */
    public String setupCommands(String dbName, String username, String password) {
        String configDir = "sudo mkdir -p /etc/mailadmin && sudo chown root:tomcat /etc/mailadmin && sudo chmod 2770 /etc/mailadmin\n\n";
        if (isPostgres()) {
            return configDir
                    + "sudo -u postgres psql -c \"CREATE DATABASE \\\"" + dbName + "\\\";\"\n\n"
                    + "sudo -u postgres psql -c \"CREATE USER \\\"" + username + "\\\" WITH PASSWORD '" + password + "';\"\n\n"
                    + "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE \\\"" + dbName + "\\\" TO \\\"" + username + "\\\";\"\n\n"
                    + "sudo -u postgres psql -d \"" + dbName + "\" -c \"GRANT ALL ON SCHEMA public TO \\\"" + username + "\\\";\"";
        }
        // MariaDB and MySQL both use the mysql client with identical syntax.
        return configDir
                + "sudo mysql -e \"\n"
                + "CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
                + "CREATE USER IF NOT EXISTS '" + username + "'@'localhost' IDENTIFIED BY '" + password + "';\n"
                + "GRANT ALL PRIVILEGES ON `" + dbName + "`.* TO '" + username + "'@'localhost';\n"
                + "FLUSH PRIVILEGES;\"";
    }
}
