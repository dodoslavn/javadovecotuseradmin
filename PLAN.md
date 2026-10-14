# tomcatdovecotuseradmin — Project Plan

## Goal
A web UI to manage Dovecot mail accounts for one mail server, without that server depending
on a database — it keeps reading a local flat file (`/etc/dovecot/passdb`), synced down by a
separate agent. The agent itself (`agent/sync-agent.pl`) is built and verified against a local
test broker/DB (see "Mail-sync agent" below); wiring it into the install scripts and pointing
it at production credentials is still outstanding (see "Deferred").

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
| Sync agent language | Perl (`agent/sync-agent.pl`) | Runs on the mail server itself; apt has everything needed (`libanyevent-rabbitmq-perl`, `libdbi-perl`, `libdbd-mariadb-perl`) with zero vendoring, unlike the Python option considered and rejected first |

## Key Decisions
- **Roles** — `ADMIN` (manage web accounts, mail accounts, pairings, server config) + `USER` (self-service: view/rotate password/enable/disable only their own linked mail account(s))
- **Pairing** — the underlying table (`web_account_dovecot_account`) is many-to-many, but there's no dedicated "links" page — each mail account on the Emails page has a single dropdown picking the one web account it's paired with (`LinkDao.setPairing()` clears any prior pairing for that mail account before setting the new one, so the UI's "one at a time" model holds even though the schema itself doesn't enforce it)
- **Server config lives in a file, not the database** — `/etc/mailadmin/config.properties` holds DB, RabbitMQ, and Sentry settings. DB settings are host-file-only (the app can't safely rewrite its own DB credentials before it can connect); RabbitMQ and Sentry settings are editable from the admin UI, writing back to that same file via an in-place line rewrite (comments and other keys preserved). Sentry DSN changes take effect immediately (`SentryReporter.reinit()`) — no restart needed
- **First-run setup wizard** — if DB credentials aren't in the config file, every request redirects to `/setup`: choose a DB name, get shown the exact `mysql`/`mkdir` commands to run, confirm connection details, and either adopt an existing installation's data untouched or create the first admin account
- **Schema versioning** — a single-row `schema_version` table tracks the installed schema version. `SchemaInstaller.ensureSchema()` runs on every app startup (`AppStartupListener`, once DB credentials exist) as well as during setup, and applies any pending migrations automatically. Upgrading to a new WAR version with schema changes is just: bump `SchemaInstaller.CURRENT_VERSION`, add a `case` to `runMigration()`, deploy, restart Tomcat — no manual `ALTER TABLE` needed on the live database. Currently at version 4 (v3 added `sync_status.last_request_sent_at`; v4 added the `web_account_invites` table — both via `CREATE TABLE IF NOT EXISTS` / `ALTER TABLE` migrations, also in `BASE_TABLES` for fresh installs)
- **Invite links** — an admin can generate a single-use, time-limited link (`/admin/web-accounts/invite`) that lets a new person create their *own* web account: they open the public `/invite?token=...` page (exempted from `AuthFilter`), choose their own username + password, and the account is created with the role the admin preset on the invite. Token is 256 bits of URL-safe entropy (so no rate-limiting needed on the public endpoint); `InviteDao.markUsedIfPending()` is a conditional UPDATE so two people racing the same link can't both succeed. The admin page also lists still-pending invites with copy-link + revoke. The invitee's own account creation is audited with them as the actor (`AuditService.recordActor`, since there's no session yet)
- **Sync status tracks two separate timestamps** — "last successfully sent sync request" (`sync_status.last_request_sent_at`, set by `RabbitMqPublisher` right after a successful publish, manual or automatic) and "last successful full sync" (`sync_status.last_sync_at`/`status`/`detail`, set by `SyncStatusRabbitMqConsumer` from the agent's report). They're deliberately not conflated — sending the request doesn't mean the agent has acted on it yet. The agent's own `detail` text (which includes an account count) is only shown on the Server configuration page when `status` is `error`; the website has no way to independently verify a count the agent self-reports, so showing it on success implied more certainty than actually exists
- **Audit log** — every mutating action is recorded with actor/action/target, but never a password or hash value
- **Mail-sync agent (`agent/sync-agent.pl`, Perl)** — runs on the mail server itself, separately from the website. Consumes the `account.changed` signal from the website's exchange (topic, routing key `account.changed`), debounces bursts into one run (`sync.debounce_seconds`, default 5s — restarts the timer on every signal received during the window), then **reads the account list directly from MariaDB** rather than over an HTTP API — a narrowly-scoped read-only DB user (`GRANT SELECT ON mailadmin.dovecot_accounts` only, nothing else, not even `web_accounts`) in the agent's own config file. HTTPS/an API was the original plan, but since the website and the mail server are the same box (one website for one mail server), that would've meant TLS/cert management for a loopback call plus a second auth mechanism — exactly what the reverse-notification redesign (below) had already moved away from once. Writes `passdb` atomically (temp file in the same directory, then `rename()`, never edited in place), then runs `doveadm reload` (configurable via `dovecot.reload_command`, in case a host's setup differs) and publishes the result back on `sync.status`. Config lives in `/etc/mailadmin/sync-agent.conf` — same folder and same simple `key=value` format as the website's `config.properties`, but `600` not `660` since it holds a live DB password with no process other than the agent (running as root, for passdb/doveadm privileges) needing it. Systemd unit template at `agent/mailadmin-sync-agent.service`. **Verified end-to-end on the local test broker**: ran the real script against a fresh narrowly-scoped MariaDB user and a fresh RabbitMQ user, published `account.changed` via `rabbitmqadmin` — confirmed (a) a 4-message rapid burst collapsed into exactly one sync run, not four, (b) the written test passdb file had the correct `user:{SHA512-CRYPT}$6$...` content and `640` permissions, (c) the `ok` status it published was picked up by the website's own already-running `SyncStatusRabbitMqConsumer` and landed correctly in the real `sync_status` table — true cross-component round trip, not a mocked one, and (d) a forced `doveadm reload` failure (Dovecot is stopped on this dev host) was caught and reported as an `error` status with the real Dovecot error text as detail, not a crash. One real bug was caught and fixed during this: the first version called `publish()` on the connection object instead of the channel object returned by `open_channel()` — AMQP operations live on the channel, not the top-level connection — which failed silently into the event loop's default error handler until the channel reference was saved and used instead. A second bug (object getting garbage-collected mid-handshake, spinning the CPU in a silent reconnect loop) was fixed by keeping a strong reference to the connecting client object outside the callback closures. All test RabbitMQ/MariaDB users were deleted after verification.
- **Reverse notification (built ahead of the agent itself), via RabbitMQ** — `SyncStatusRabbitMqConsumer` runs as a background thread (started/stopped with the app, restarted immediately if RabbitMQ settings change) consuming a durable queue (`mailadmin.sync-status`, bound to the configured exchange with routing key `sync.status`) that the agent publishes to after each run. Message body is deliberately not JSON — first line `ok`/`error`, optional detail text after — so the agent needs zero dependencies to send one. Reuses the same `rabbitmq.*` credentials already configured for the outbound "something changed" signal; no separate auth mechanism, unlike an earlier version of this that used a bearer-token HTTP endpoint. Shown on Server configuration as "last sync: time + status badge". Own reconnect-with-backoff loop (10s) rather than amqp-client's built-in automatic recovery, so there's exactly one recovery mechanism to reason about. Verified end-to-end against a real broker: installed `rabbitmq-server` locally on Deb-AI (dev-only, not production rabbitmq.fordo.eu — those real credentials still aren't known), created a test vhost/user, pointed the app at it, and published `ok`/`error`/malformed test messages via the management HTTP API — all three handled correctly (recorded, recorded with detail, and rejected-without-corrupting-state respectively). Confirmed the exchange/queue/binding the consumer declares match the design exactly. Config was reverted to the real (still-unverified) rabbitmq.fordo.eu values afterward; the local test broker was left running for future dev use.
- **Connection status + manual trigger** — Server configuration shows a live Connected/Disconnected badge for the RabbitMQ connection (`SyncStatusRabbitMqConsumer.isConnected()`), and a "Force sync now" button always publishes the outbound "something changed" signal immediately (`RabbitMqPublisher.publishSyncRequest()`, routing key `account.changed`, payload `"manual trigger by <username>"` — no account data), independent of the Auto-sync setting below. Verified against the local test broker: a bound observer queue confirmed the exact payload lands correctly; pointing at an unreachable host showed the error flash instead of a 500.
- **Auto-sync (optional, off by default)** — a checkbox on Server configuration (`sync.autosync` in config.properties) controls whether mail-account changes publish `account.changed` automatically, instead of only via "Force sync now". `RabbitMqPublisher.publishIfAutoSyncEnabled(reason)` checks the flag and swallows (Sentry-reports) any publish failure rather than letting a RabbitMQ hiccup break the account action that triggered it. Wired into exactly the mutations that change what `sync-agent.pl` actually reads (`SELECT ... FROM dovecot_accounts WHERE status='active'`) — create/enable/disable/delete in `DovecotAccountServlet` and the self-service enable/disable/password-change in `MyAccountsServlet` — deliberately **not** `WebAccountServlet` (web logins aren't in passdb) or `pair()` (pairing doesn't affect passdb content either). Toggle changes are audited (`sync_autosync_updated`). Verified live against the local test broker with a bound observer queue: turning Auto-sync on and disabling a mail account published `"auto: disabled <address>"` immediately; turning it back off (done concurrently by a second, real browser session mid-test) correctly stopped the very next action from publishing anything — confirms the flag takes effect immediately, no restart needed, same as every other setting in this file.

## Repository Structure
```
tomcatdovecotuseradmin/
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
├── agent/                                   # Runs on the mail server, not deployed by install/ yet
│   ├── sync-agent.pl                        # Perl — see "Mail-sync agent" above
│   ├── sync-agent.conf.example              # Template for /etc/mailadmin/sync-agent.conf
│   └── mailadmin-sync-agent.service         # systemd unit template
└── website/
    ├── build.sh                             # javac + jar, no Maven/Gradle
    ├── sql/schema.sql
    └── src/main/
        ├── java/eu/fordo/mailadmin/
        │   ├── model/       # WebAccount, DovecotAccount, AuditLogEntry, Role
        │   ├── dao/          # Database, WebAccountDao, DovecotAccountDao, LinkDao, AuditLogDao
        │   ├── service/      # PasswordService, AppConfig, SessionUtil, AuditService, Messages, SentryReporter, SchemaInstaller
        │   └── servlet/      # one servlet per route + AuthFilter, LocaleFilter, SetupGateFilter, SetupServlet
        ├── resources/i18n/   # messages_en.properties + cs/de/es/hu/it/pl/sk/uk — same language set as phpopenvpnadmin
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

## Resilience (tested live, not just reasoned about)
- **`/etc/mailadmin/config.properties` deleted while the app is running**: zero immediate effect — `AppConfig` only reads from disk at class-load and on explicit `reload()`/`update()`, so the in-memory settings (and the running app) are unaffected by the file vanishing. The real risk was the *next* save: `AppConfig.update()` used to merge new values against whatever was on disk, so a save after the file disappeared would silently rewrite it containing *only* the keys from that one save — db.*, sentry.dsn, everything else, gone, both from the file and (since `update()` reloads after writing) from the running app's memory too, which then locked the admin out (`SetupGateFilter` saw no DB credentials and redirected everything to `/setup`). **Fixed**: `update()` now falls back to reconstructing from the in-memory cache when the file isn't readable, so a save after an accidental deletion heals the file back to a complete state (comments and key order are lost in that fallback path, but no data loss). Confirmed the fix live: delete the file, save RabbitMQ settings, every other key (db.*, sentry.dsn, sync.token) survived and the app never got locked out.
- **DB connection fails while the app is already running**: every DB-dependent request (including a fresh login POST) degrades to the generic error page (500, no stack trace leaked); pages that don't touch the DB (e.g. the login page's GET) keep working. A RabbitMQ message that arrives during the outage is `basicNack`'d with requeue (confirmed via queue depth), not lost, and gets reprocessed automatically the moment the DB comes back — no restart needed either way.
- **DB connection fails during app startup**: Tomcat still deploys and starts successfully; only schema migration for that boot is skipped (now logged loudly — see `AppStartupListener`'s `java.util.logging` call, added specifically because an earlier version of this relied on Sentry alone, which silently swallows it without a configured DSN). The RabbitMQ consumer starts regardless, since it doesn't depend on that check. Since no migration was actually pending in this test, impact was limited to the log line; a genuinely pending migration would stay unapplied until the next full restart, not auto-retried — worth knowing if a future deploy needs both a schema change and happens to land while the DB is flaky.

## Deferred
- Creating the real `mailsync` MariaDB user and the real `/etc/mailadmin/sync-agent.conf` on the actual mail server (the ones used for verification were test-only and deleted afterward)
- An install step and systemd unit install for the agent (`agent/mailadmin-sync-agent.service` is a template, not wired into `install/steps/` yet)
- Pointing the agent at production RabbitMQ credentials (rabbitmq.fordo.eu) — still unverified, same as the website's own RabbitMQ config
- A live end-to-end test against a real running Dovecot instance (this dev host has `doveadm` only, service stopped+disabled — the agent's DB pull, debounce, atomic file write, and status reporting are verified; the actual `doveadm reload` success path against a live Dovecot is not)

Scope is one website managing one mail server — no host/domain field needed on accounts, and no multi-host routing concern for the sync phase.
