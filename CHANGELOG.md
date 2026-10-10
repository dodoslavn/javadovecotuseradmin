# Changelog

## 0.1.0 — Initial build

First working version of the admin website (step 1 of the broader mail-account-management
plan — the RabbitMQ-driven sync agent that regenerates Dovecot's `passdb` file on the mail
hosts themselves is a later, not-yet-built phase).

**Core**
- Login, session-based auth, two roles: `ADMIN` and `USER`
- `ADMIN`: manage web logins ("Manage accesses"), Dovecot mail accounts ("Emails"), the
  many-to-many link between them, and view the audit log
- `USER`: self-service view/rotate-password/enable/disable of only their own linked mail
  account(s) — enforced server-side on every action, not just hidden in the UI
- Every mutating action recorded in an audit log (actor, action, target) — never a password
  or hash value
- `/account` — self-service change of one's own **web login** password (separate from mail
  account passwords, which use a different hashing path entirely)

**First-run setup wizard** (`/setup`)
- Gated on whether `/etc/mailadmin/config.properties` has DB credentials yet
- Choose a database name → shown the exact commands to create it → confirm connection
  details → schema created if missing (existing installations' data is left untouched) →
  first admin account created if none exists yet

**Server configuration**
- DB, RabbitMQ, and Sentry settings live in `/etc/mailadmin/config.properties` on the host,
  not the database (the app can't safely rewrite its own DB credentials)
- RabbitMQ section is editable from the admin UI, written back to that same file in place
- MySQL/MariaDB section is read-only in the UI

**i18n**
- Resource-bundle-backed message lookup (`Messages`, `LocaleFilter`); English only today,
  but a `?lang=` switch and per-session persistence are already wired for more languages later

**Stack**
- Java, plain Servlets + JSP on Tomcat 10 (no framework — not realistically apt-only)
- MariaDB for the app's own data; bcrypt for web logins; real `doveadm pw -s SHA512-CRYPT`
  (via stdin, not argv) for mail-account hashes
- Sentry error reporting (vendored jar — the one deliberate exception to the apt-only
  dependency policy)
- Visual style, nav/footer structure, and the numbered idempotent installer
  (`install/install.sh` + `install/steps/*.sh`) all ported from the `phpopenvpnadmin` sister
  project for consistency across this household's admin tools

**Deferred**
- RabbitMQ change-signal publishing and the per-mail-host Python sync agent that actually
  regenerates `/etc/dovecot/passdb` and reloads Dovecot — storage for RabbitMQ config exists,
  nothing publishes or consumes yet
