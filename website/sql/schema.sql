-- Mail Account Admin Website — schema (MariaDB)
CREATE DATABASE IF NOT EXISTS mailadmin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mailadmin;

CREATE TABLE web_accounts (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN', 'USER') NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Mirrors the real Dovecot passdb-file format this app targets: just "username:password".
-- No uid/gid/home — those belong to userdb (often static/shared), not passdb, in a typical
-- Dovecot setup, so they have no home here either.
CREATE TABLE dovecot_accounts (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    domain        VARCHAR(100) NOT NULL,
    username      VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        ENUM('active', 'disabled') NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_domain_username (domain, username)
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
