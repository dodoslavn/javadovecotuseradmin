-- Mail Account Admin Website — schema (MariaDB/MySQL reference).
-- This file is a human-readable reference for the MySQL/MariaDB dialect. The app does NOT run it;
-- SchemaInstaller.java creates and migrates the schema at runtime for whichever database system
-- was chosen in the setup wizard (MariaDB, MySQL, or PostgreSQL) — see SchemaInstaller for the
-- PostgreSQL dialect (IDENTITY, VARCHAR+CHECK instead of ENUM, updated_at triggers, etc.).
CREATE DATABASE IF NOT EXISTS mailadmin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mailadmin;

CREATE TABLE web_accounts (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN', 'USER') NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    language      VARCHAR(8) NULL,      -- preferred UI language code; NULL = follow browser/cookie (see LocaleFilter)
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Mirrors the real Dovecot passdb-file format this app targets: just "username:password".
-- No uid/gid/home (see below) — and no separate domain column either: username is expected
-- to already be the full address (user@domain), so a domain column would just duplicate
-- what's parseable from username.
CREATE TABLE dovecot_accounts (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status        ENUM('active', 'disabled') NOT NULL DEFAULT 'active',
    mail_folder   VARCHAR(255) NULL,     -- Dovecot userdb mail location; several addresses may share one (aliases). See MailFolder / sync-agent.
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE web_account_dovecot_account (
    web_account_id     BIGINT NOT NULL,
    dovecot_account_id BIGINT NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (web_account_id, dovecot_account_id),
    FOREIGN KEY (web_account_id) REFERENCES web_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (dovecot_account_id) REFERENCES dovecot_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- RabbitMQ and DB connection settings are NOT stored here — they live in
-- /etc/mailadmin/config.properties on the host (see AppConfig.java), edited directly by
-- someone with server access, not through the web UI. See the "Server configuration" admin page.

CREATE TABLE audit_log (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_web_account_id  BIGINT NULL,
    action                VARCHAR(64) NOT NULL,
    target_type           VARCHAR(32) NOT NULL,
    target_id             BIGINT NULL,
    detail                VARCHAR(500) NULL,
    at                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_web_account_id) REFERENCES web_accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Tracks which version of this schema is installed. SchemaInstaller.java checks this on
-- every app startup (once DB credentials exist) and runs any pending migrations
-- automatically — see SchemaInstaller.CURRENT_VERSION. A fresh install just seeds this
-- directly at the current version rather than replaying history.
CREATE TABLE schema_version (
    id         TINYINT PRIMARY KEY DEFAULT 1,
    version    INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT single_row CHECK (id = 1)
) ENGINE=InnoDB;
INSERT INTO schema_version (id, version) VALUES (1, 6);

-- Single-use, time-limited invite links. An admin generates one; whoever opens
-- /invite?token=... chooses their own username + password and a web account is created with
-- the preset role. token is 256 bits of URL-safe entropy; used_at (single-use) and expires_at
-- are both checked on accept. See InviteDao / InviteServlet / InviteAdminServlet.
CREATE TABLE web_account_invites (
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
) ENGINE=InnoDB;

-- last_sync_at/status/detail: reported by the sync agent (agent/sync-agent.pl, on the mail
-- server) over RabbitMQ after each run — see SyncStatusRabbitMqConsumer. Shown on the
-- "Server configuration" admin page as "Last successful full sync".
-- last_request_sent_at: when the website itself last successfully published the
-- "something changed" signal (RabbitMqPublisher) — either the "Force sync now" button or,
-- if enabled, an automatic mail-account change. Distinct from last_sync_at because sending
-- the request doesn't mean the agent has finished (or even started) acting on it yet.
CREATE TABLE sync_status (
    id                    TINYINT PRIMARY KEY DEFAULT 1,
    last_sync_at          TIMESTAMP NULL,
    status                VARCHAR(16) NULL,
    detail                VARCHAR(500) NULL,
    last_request_sent_at  TIMESTAMP NULL,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT single_row CHECK (id = 1)
) ENGINE=InnoDB;
