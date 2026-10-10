# javaDovecotAdmin — Project Plan

## Goal
A web UI to manage Dovecot mail accounts across one or more independent mail servers (any
number of domains/hosts — "domain" is a free-text field, not a fixed enum), without the mail
servers themselves depending on a database — they keep reading local flat
files (`/etc/dovecot/passdb`), synced down by a separate agent later. This repo covers the
website only; the sync agent is a deferred phase (see below).

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
| Dependency policy | apt packages only, build with `javac`/`jar` directly | No Maven/Gradle at build or run time |

## Key Decisions
- **Roles** — `ADMIN` (manage web accounts, mail accounts, links, server config) + `USER` (self-service: view/rotate password/enable/disable only their own linked mail account(s))
- **Relation table** — a web account and a Dovecot mail account are linked many-to-many via `web_account_dovecot_account`, not a direct foreign key
- **Server config lives in a file, not the database** — `/etc/mailadmin/config.properties` holds DB, RabbitMQ, and Sentry settings. DB settings are host-file-only (the app can't safely rewrite its own DB credentials before it can connect); RabbitMQ settings are additionally editable from the admin UI, writing back to that same file via an in-place line rewrite (comments and other keys preserved)
- **First-run setup wizard** — if DB credentials aren't in the config file, every request redirects to `/setup`: choose a DB name, get shown the exact `mysql`/`mkdir` commands to run, confirm connection details, and either adopt an existing installation's data untouched or create the first admin account
- **Audit log** — every mutating action is recorded with actor/action/target, but never a password or hash value
- **Mail-sync plan (deferred)** — RabbitMQ carries only a lightweight "something changed" signal (never account data), each mail host's agent debounces it, pulls the current account list over an authenticated HTTPS API, and atomically regenerates `passdb` — see the architecture discussion for the full design; not implemented in this repo yet

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
dovecot_accounts              (id, domain, username, password_hash, status, created_at, updated_at)
web_account_dovecot_account   (web_account_id, dovecot_account_id, created_at)   -- many-to-many
audit_log                     (id, actor_web_account_id, action, target_type, target_id, detail, at)
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
- An admin cannot disable their own web account (`webAccounts.error.selfDisable`)
- `/etc/mailadmin/config.properties` is `root:tomcat`, mode `660`/`2770` — readable and (for the RabbitMQ section only) writable by the Tomcat process, nothing else
- Tomcat's systemd unit keeps `ProtectSystem=strict`; only `/etc/mailadmin` is carved out via `ReadWritePaths`

## Deferred
RabbitMQ change-signal publishing, the per-mail-host Python sync agent, atomic `passdb` file regeneration, Dovecot reload. Config storage for RabbitMQ is already in place; the agent and publisher are not built yet.
