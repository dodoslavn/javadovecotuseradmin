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
 *
 * Dialect: the DDL comes in two flavours, chosen from the live connection's JDBC URL — the
 * MySQL/MariaDB family and PostgreSQL (see DbSystem). They differ in auto-increment syntax
 * (AUTO_INCREMENT vs IDENTITY), enums (inline ENUM vs VARCHAR + CHECK), the updated_at
 * auto-touch (ON UPDATE clause vs a trigger), the single-row PK type (TINYINT vs SMALLINT),
 * the storage-engine suffix (ENGINE=InnoDB vs none), and upsert syntax (ON DUPLICATE KEY vs
 * ON CONFLICT). Everything else is plain portable SQL.
 */
public final class SchemaInstaller {

    /** Bump this and add a case in runMigration() whenever the schema changes. */
    public static final int CURRENT_VERSION = 6;

    private static final String[] MYSQL_TABLES = {
            """
            CREATE TABLE IF NOT EXISTS web_accounts (
                id            BIGINT PRIMARY KEY AUTO_INCREMENT,
                username      VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role          ENUM('ADMIN', 'USER') NOT NULL,
                enabled       BOOLEAN NOT NULL DEFAULT TRUE,
                language      VARCHAR(8) NULL,
                created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS dovecot_accounts (
                id            BIGINT PRIMARY KEY AUTO_INCREMENT,
                username      VARCHAR(190) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                status        ENUM('active', 'disabled') NOT NULL DEFAULT 'active',
                mail_folder   VARCHAR(255) NULL,
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
            """,
            """
            CREATE TABLE IF NOT EXISTS sync_status (
                id                    TINYINT PRIMARY KEY DEFAULT 1,
                last_sync_at          TIMESTAMP NULL,
                status                VARCHAR(16) NULL,
                detail                VARCHAR(500) NULL,
                last_request_sent_at  TIMESTAMP NULL,
                updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT single_row CHECK (id = 1)
            ) ENGINE=InnoDB
            """,
            """
            CREATE TABLE IF NOT EXISTS web_account_invites (
                id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
                token                     VARCHAR(64) NOT NULL UNIQUE,
                role                      ENUM('ADMIN', 'USER') NOT NULL,
                suggested_username        VARCHAR(100) NULL,
                created_by_web_account_id BIGINT NULL,
                created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at                TIMESTAMP NOT NULL,
                used_at                   TIMESTAMP NULL,
                used_web_account_id       BIGINT NULL,
                FOREIGN KEY (created_by_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL,
                FOREIGN KEY (used_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL
            ) ENGINE=InnoDB
            """
    };

    private static final String[] POSTGRES_TABLES = {
            """
            CREATE TABLE IF NOT EXISTS web_accounts (
                id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                username      VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role          VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
                enabled       BOOLEAN NOT NULL DEFAULT TRUE,
                language      VARCHAR(8) NULL,
                created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS dovecot_accounts (
                id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                username      VARCHAR(190) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                status        VARCHAR(10) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'disabled')),
                mail_folder   VARCHAR(255) NULL,
                created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS web_account_dovecot_account (
                web_account_id     BIGINT NOT NULL,
                dovecot_account_id BIGINT NOT NULL,
                created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (web_account_id, dovecot_account_id),
                FOREIGN KEY (web_account_id) REFERENCES web_accounts(id) ON DELETE CASCADE,
                FOREIGN KEY (dovecot_account_id) REFERENCES dovecot_accounts(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                id                    BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                actor_web_account_id  BIGINT NULL,
                action                VARCHAR(64) NOT NULL,
                target_type           VARCHAR(32) NOT NULL,
                target_id             BIGINT NULL,
                detail                VARCHAR(500) NULL,
                at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (actor_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS schema_version (
                id         SMALLINT PRIMARY KEY DEFAULT 1,
                version    INT NOT NULL,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT single_row CHECK (id = 1)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS sync_status (
                id                    SMALLINT PRIMARY KEY DEFAULT 1,
                last_sync_at          TIMESTAMP NULL,
                status                VARCHAR(16) NULL,
                detail                VARCHAR(500) NULL,
                last_request_sent_at  TIMESTAMP NULL,
                updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT single_row CHECK (id = 1)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS web_account_invites (
                id                        BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                token                     VARCHAR(64) NOT NULL UNIQUE,
                role                      VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
                suggested_username        VARCHAR(100) NULL,
                created_by_web_account_id BIGINT NULL,
                created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at                TIMESTAMP NOT NULL,
                used_at                   TIMESTAMP NULL,
                used_web_account_id       BIGINT NULL,
                FOREIGN KEY (created_by_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL,
                FOREIGN KEY (used_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL
            )
            """
    };

    // Postgres has no "ON UPDATE CURRENT_TIMESTAMP"; a BEFORE UPDATE trigger gives updated_at the
    // same auto-touch behaviour. The function is shared; one trigger per table that has the column.
    private static final String PG_UPDATED_AT_FUNCTION =
            "CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$ " +
            "BEGIN NEW.updated_at = CURRENT_TIMESTAMP; RETURN NEW; END; $$ LANGUAGE plpgsql";
    private static final String[] PG_UPDATED_AT_TABLES = {"dovecot_accounts", "schema_version", "sync_status"};

    private SchemaInstaller() {}

    public static void ensureSchema(Connection conn) throws SQLException {
        boolean postgres = DbSystem.urlIsPostgres(conn.getMetaData().getURL());

        try (Statement st = conn.createStatement()) {
            for (String sql : (postgres ? POSTGRES_TABLES : MYSQL_TABLES)) {
                st.execute(sql);
            }
            if (postgres) {
                st.execute(PG_UPDATED_AT_FUNCTION);
                for (String table : PG_UPDATED_AT_TABLES) {
                    st.execute("DROP TRIGGER IF EXISTS trg_set_updated_at_" + table + " ON " + table);
                    st.execute("CREATE TRIGGER trg_set_updated_at_" + table + " BEFORE UPDATE ON " + table
                            + " FOR EACH ROW EXECUTE FUNCTION set_updated_at()");
                }
            }
        }

        Integer storedVersion = readVersion(conn);
        if (storedVersion == null) {
            // Fresh install, or an existing install from before version tracking existed —
            // either way the base tables just created above are already at CURRENT_VERSION's
            // shape, so seed directly rather than replaying migrations against them.
            setVersion(conn, CURRENT_VERSION, postgres);
        } else if (storedVersion < CURRENT_VERSION) {
            int version = storedVersion;
            while (version < CURRENT_VERSION) {
                version++;
                runMigration(conn, version);
                setVersion(conn, version, postgres);
            }
        }
    }

    private static void runMigration(Connection conn, int toVersion) throws SQLException {
        // The ALTERs below are plain portable SQL (valid on both dialects). Note PostgreSQL
        // support arrived at v5, so no Postgres install is ever below it — these migrations only
        // ever actually run on existing MySQL/MariaDB installs. They stay dialect-agnostic anyway.
        try (Statement st = conn.createStatement()) {
            switch (toVersion) {
                // sync_status is a brand-new table, already created above by the base tables' own
                // CREATE TABLE IF NOT EXISTS — nothing further needed for an existing install.
                case 2 -> { }
                case 3 -> st.execute("ALTER TABLE sync_status ADD COLUMN last_request_sent_at TIMESTAMP NULL");
                // web_account_invites is a brand-new table, already created above by the base
                // tables' own CREATE TABLE IF NOT EXISTS — nothing further needed.
                case 4 -> { }
                // Per-account preferred UI language (null = follow browser/cookie). See LocaleFilter.
                case 5 -> st.execute("ALTER TABLE web_accounts ADD COLUMN language VARCHAR(8) NULL");
                // Per-mailbox folder (Dovecot userdb mail location). Several addresses may share one
                // folder (aliases). null = not set; the sync agent skips userdb lines without a folder.
                case 6 -> st.execute("ALTER TABLE dovecot_accounts ADD COLUMN mail_folder VARCHAR(255) NULL");
                default -> throw new IllegalStateException("No migration defined for schema version " + toVersion);
            }
        }
    }

    private static Integer readVersion(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM schema_version WHERE id = 1")) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private static void setVersion(Connection conn, int version, boolean postgres) throws SQLException {
        String sql = postgres
                ? "INSERT INTO schema_version (id, version) VALUES (1, " + version + ") "
                        + "ON CONFLICT (id) DO UPDATE SET version = " + version
                : "INSERT INTO schema_version (id, version) VALUES (1, " + version + ") "
                        + "ON DUPLICATE KEY UPDATE version = " + version;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
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
