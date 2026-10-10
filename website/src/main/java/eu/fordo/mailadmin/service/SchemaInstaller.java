package eu.fordo.mailadmin.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Idempotent schema creation for the setup wizard — mirrors sql/schema.sql (minus the
 * CREATE DATABASE line; the wizard connects to an already-created database by that point).
 * Safe to run against a brand-new empty database or an already-installed one: every
 * statement is IF NOT EXISTS, so a previous installation's data is never touched.
 */
public final class SchemaInstaller {

    private static final String[] STATEMENTS = {
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
            """
    };

    private SchemaInstaller() {}

    public static void ensureSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            for (String sql : STATEMENTS) {
                st.execute(sql);
            }
        }
    }

    public static boolean hasAnyWebAccount(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM web_accounts")) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }
}
