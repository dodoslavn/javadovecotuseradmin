package eu.fordo.mailadmin.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Idempotent schema creation AND versioned migration, run both by the setup wizard (fresh
 * install) and on every app startup (AppStartupListener) once DB credentials exist — so
 * upgrading to a new WAR version with schema changes just means restarting Tomcat with the
 * new version, no manual ALTER TABLE required.
 *
 * To ship a schema change: bump CURRENT_VERSION and add a case to runMigration() for the new
 * number. Existing installs get migrated step by step from whatever version they're on up to
 * CURRENT_VERSION; a brand-new install just gets the base tables below (already at the latest
 * shape) and has its version row seeded directly, skipping the migration steps entirely.
 */
public final class SchemaInstaller {

    /** Bump this and add a case in runMigration() whenever the schema changes. */
    public static final int CURRENT_VERSION = 1;

    private static final String[] BASE_TABLES = {
            """
            CREATE TABLE IF NOT EXISTS web_accounts (
                id            BIGINT PRIMARY KEY AUTO_INCREMENT,
                username      VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role          ENUM('ADMIN', 'USER') NOT NULL,
                enabled       BOOLEAN NOT NULL DEFAULT TRUE,
                created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS dovecot_accounts (
                id            BIGINT PRIMARY KEY AUTO_INCREMENT,
                username      VARCHAR(190) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                status        ENUM('active', 'disabled') NOT NULL DEFAULT 'active',
                created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS web_account_dovecot_account (
                web_account_id     BIGINT NOT NULL,
                dovecot_account_id BIGINT NOT NULL,
                created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (web_account_id, dovecot_account_id),
                FOREIGN KEY (web_account_id) REFERENCES web_accounts(id) ON DELETE CASCADE,
                FOREIGN KEY (dovecot_account_id) REFERENCES dovecot_accounts(id) ON DELETE CASCADE
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
                actor_web_account_id  BIGINT NULL,
                action                VARCHAR(64) NOT NULL,
                target_type           VARCHAR(32) NOT NULL,
                target_id             BIGINT NULL,
                detail                VARCHAR(500) NULL,
                at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (actor_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS schema_version (
                id         TINYINT PRIMARY KEY DEFAULT 1,
                version    INT NOT NULL,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT single_row CHECK (id = 1)
            ) ENGINE=InnoDB
            """
    };

    private SchemaInstaller() {}

    public static void ensureSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            for (String sql : BASE_TABLES) {
                st.execute(sql);
            }
        }

        Integer storedVersion = readVersion(conn);
        if (storedVersion == null) {
            // Fresh install, or an existing install from before version tracking existed —
            // either way the base tables just created above are already at CURRENT_VERSION's
            // shape, so seed directly rather than replaying migrations against them.
            setVersion(conn, CURRENT_VERSION);
        } else if (storedVersion < CURRENT_VERSION) {
            int version = storedVersion;
            while (version < CURRENT_VERSION) {
                version++;
                runMigration(conn, version);
                setVersion(conn, version);
            }
        }
    }

    private static void runMigration(Connection conn, int toVersion) throws SQLException {
        switch (toVersion) {
            // case 2 -> { try (Statement st = conn.createStatement()) { st.execute("ALTER TABLE ..."); } }
            default -> throw new IllegalStateException("No migration defined for schema version " + toVersion);
        }
    }

    private static Integer readVersion(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_version WHERE id = 1")) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private static void setVersion(Connection conn, int version) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO schema_version (id, version) VALUES (1, " + version + ") " +
                    "ON DUPLICATE KEY UPDATE version = " + version);
        }
    }

    public static boolean hasAnyWebAccount(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM web_accounts")) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }
}
