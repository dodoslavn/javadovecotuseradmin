# javaDovecotAdmin — Project Plan

## Goal
A web UI to manage Dovecot mail accounts for one mail server, without that server depending
on a database — it keeps reading a local flat file (`/etc/dovecot/passdb`), synced down by a
separate agent later. This repo covers the website only; the sync agent is a deferred phase
(see below).

## Stack
| Component | Choice | Reason |
|---|---|---|
| OS | Debian 13 (Trixie) | Latest stable Debian |
| Web server | Tomcat 10 | Already running elsewhere in this environment; Jakarta namespace |
| Language | Java, plain Servlets + JSP | No framework — Spring Boot etc. isn't realistically apt-only |
| Database | MariaDB | Full DB server; app has its own credentials, unrelated to Dovecot's flat files |
| Password hashing (web login) | bcrypt (`libjbcrypt-java`) | apt-packaged, no exception needed |
| Password hashing (mail accounts) | `doveadm pw -s SHA512-CRYPT` via `ProcessBuilder` | Must match whatever Dovecot itself accepts |
| Error reporting | Sentry | No apt package for Java — the one deliberate exception to apt-only; vendored jar in `WEB-INF/lib` |
| Messaging | RabbitMQ Java client (`librabbitmq-client-java`) | apt-packaged; its `libslf4j-java` dependency's jar is deliberately *not* copied into `WEB-INF/lib` — reuses the newer vendored slf4j-api already there for Sentry instead, to avoid two major versions on one classpath |
| Dependency policy | apt packages only, build with `javac`/`jar` directly | No Maven/Gradle at build or run time |

## Key Decisions
- **Roles** — `ADMIN` (manage web accounts, mail accounts, pairings, server config) + `USER` (self-service: view/rotate password/enable/disable only their own linked mail account(s))
- **Pairing** — the underlying table (`web_account_dovecot_account`) is many-to-many, but there's no dedicated "links" page — each mail account on the Emails page has a single dropdown picking the one web account it's paired with (`LinkDao.setPairing()` clears any prior pairing for that mail account before setting the new one, so the UI's "one at a time" model holds even though the schema itself doesn't enforce it)
- **Server config lives in a file, not the database** — `/etc/mailadmin/config.properties` holds DB, RabbitMQ, and Sentry settings. DB settings are host-file-only (the app can't safely rewrite its own DB credentials before it can connect); RabbitMQ and Sentry settings are editable from the admin UI, writing back to that same file via an in-place line rewrite (comments and other keys preserved). Sentry DSN changes take effect immediately (`SentryReporter.reinit()`) — no restart needed
- **First-run setup wizard** — if DB credentials aren't in the config file, every request redirects to `/setup`: choose a DB name, get shown the exact `mysql`/`mkdir` commands to run, confirm connection details, and either adopt an existing installation's data untouched or create the first admin account
- **Schema versioning** — a single-row `schema_version` table tracks the installed schema version. `SchemaInstaller.ensureSchema()` runs on every app startup (`AppStartupListener`, once DB credentials exist) as well as during setup, and applies any pending migrations automatically. Upgrading to a new WAR version with schema changes is just: bump `SchemaInstaller.CURRENT_VERSION`, add a `case` to `runMigration()`, deploy, restart Tomcat — no manual `ALTER TABLE` needed on the live database
- **Audit log** — every mutating action is recorded with actor/action/target, but never a password or hash value
- **Mail-sync plan (deferred)** — RabbitMQ carries only a lightweight "something changed" signal (never account data), an agent on the mail server debounces it, pulls the current account list over an authenticated HTTPS API, and atomically regenerates `passdb` — see the architecture discussion for the full design; not implemented in this repo yet
- **Reverse notification (built ahead of the agent itself), via RabbitMQ** — `SyncStatusRabbitMqConsumer` runs as a background thread (started/stopped with the app, restarted immediately if RabbitMQ settings change) consuming a durable queue (`mailadmin.sync-status`, bound to the configured exchange with routing key `sync.status`) that the future agent will publish to after each run. Message body is deliberately not JSON — first line `ok`/`error`, optional detail text after — so the eventual Python agent needs zero dependencies to send one. Reuses the same `rabbitmq.*` credentials already configured for the (also still deferred) outbound "something changed" signal; no separate auth mechanism, unlike an earlier version of this that used a bearer-token HTTP endpoint. Shown on Server configuration as "last sync: time + status badge". Own reconnect-with-backoff loop (10s) rather than amqp-client's built-in automatic recovery, so there's exactly one recovery mechanism to reason about. Not yet verified against the real broker — the `rabbitmq.*` values currently in config.properties were only ever used to test the save/update UI, not confirmed to be real credentials on rabbitmq.fordo.eu

## Repository Structure
```
javaDovecotAdmin/
├── PLAN.md
├── README.md
├── install/
│   ├── install.sh                          # Orchestrator — loops steps/*.sh in order
│   ├── lib/
│   │   └── common.sh                       # log(), error(), check_done(), mark_done()
│   └── steps/
│       ├── 00-check-root.sh
│       ├── 01-check-os.sh
│       ├── 02-check-internet.sh
│       ├── 03-apt-update.sh
│       ├── 04-install-jdk.sh
│       ├── 05-install-tomcat.sh
│       ├── 06-install-mariadb.sh
│       ├── 07-install-java-libs.sh         # servlet-api, mariadb-java, jbcrypt
│       ├── 08-install-dovecot-core.sh      # doveadm CLI only — service stopped+disabled
│       ├── 09-create-config-dir.sh         # /etc/mailadmin, root:tomcat 2770
│       ├── 10-tomcat-port-80.sh            # setcap + server.xml connector port
│       ├── 11-tomcat-systemd-override.sh   # ReadWritePaths=/etc/mailadmin
│       ├── 12-prepare-webapp-libs.sh       # copy apt jars + vendor sentry-java into WEB-INF/lib
│       ├── 13-deploy-webapp.sh             # build.sh + copy WAR into Tomcat webapps
│       └── 14-verify.sh                    # Check services/port/HTTP response, print summary
└── website/
    ├── build.sh                             # javac + jar, no Maven/Gradle
    ├── sql/schema.sql
    └── src/main/
        ├── java/eu/fordo/mailadmin/
        │   ├── model/       # WebAccount, DovecotAccount, AuditLogEntry, Role
        │   ├── dao/          # Database, WebAccountDao, DovecotAccountDao, LinkDao, AuditLogDao
        │   ├── service/      # PasswordService, AppConfig, SessionUtil, AuditService, Messages, SentryReporter, SchemaInstaller
        │   └── servlet/      # one servlet per route + AuthFilter, LocaleFilter, SetupGateFilter, SetupServlet
        ├── resources/i18n/   # messages.properties (English only today; plumbing supports more)
        └── webapp/
            ├── WEB-INF/jsp/  # one JSP per page + nav.jspf/footer.jspf fragments
            ├── WEB-INF/lib/  # mariadb-java-client.jar, jbcrypt.jar, sentry-java (vendored)
            ├── css/style.css
            └── assets/logo.svg
```

## Database Schema
```sql
web_accounts                 (id, username, password_hash, role, enabled, created_at)
dovecot_accounts              (id, username, password_hash, status, created_at, updated_at)   -- username is the full address (user@domain); no separate domain column
web_account_dovecot_account   (web_account_id, dovecot_account_id, created_at)   -- many-to-many
audit_log                     (id, actor_web_account_id, action, target_type, target_id, detail, at)
schema_version                 (id, version, updated_at)   -- single row; see "Schema versioning" above
```
RabbitMQ and DB connection settings are **not** stored in the database — see `/etc/mailadmin/config.properties` and the "Server configuration" admin page.

## First-run Wizard (`/setup`, gated by `AppConfig.hasDbCredentials()`)
1. Choose a DB system (MariaDB only, for now) and a database name
2. Shown the exact `mkdir`/`mysql` commands to create that database + a matching user, with a prefilled connection form (generated username/password matching those commands, editable)
3. On connect: `SchemaInstaller.ensureSchema()` creates any missing tables (`CREATE TABLE IF NOT EXISTS` — never touches an existing installation's data)
4. If `web_accounts` is empty, prompt for the first admin account inline; if it already has rows, skip straight to finishing
5. Write `db.url`/`db.user`/`db.password` into `/etc/mailadmin/config.properties`, redirect to `/login`

## Security
- Mail-account password hashes are generated by shelling out to the real `doveadm pw`, fed via stdin (not argv, which is visible to other local users via `/proc/<pid>/cmdline`)
- Website login passwords use bcrypt, entirely separate from the Dovecot hashing path — never cross-used
- No plaintext password is ever stored in the database or written to a log; the audit log records actions, never secret values (RabbitMQ password included)
- A `USER` role can only ever touch the Dovecot account(s) explicitly linked to their own web account — every self-service action re-checks the link, not just the session
- An admin cannot disable their own web account (`webAccounts.error.selfDisable`) or change their own role (`webAccounts.error.selfRoleChange`) — both would risk locking the only admin out
- `/etc/mailadmin/config.properties` is `root:tomcat`, mode `660`/`2770` — readable and (for the RabbitMQ section only) writable by the Tomcat process, nothing else
- Tomcat's systemd unit keeps `ProtectSystem=strict`; only `/etc/mailadmin` is carved out via `ReadWritePaths`

## Deferred
RabbitMQ change-signal publishing, the sync agent on the mail server, atomic `passdb` file regeneration, Dovecot reload. Config storage for RabbitMQ is already in place; the agent and publisher are not built yet. Scope is one website managing one mail server — no host/domain field needed on accounts, and no multi-host routing concern for the sync phase.
