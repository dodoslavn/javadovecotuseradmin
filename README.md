# javaDovecotAdmin
A web interface written in plain Java (Servlets + JSP, no framework) for managing Dovecot
mail accounts for one mail server, without that mail server depending on a database — it
keeps reading local flat files. The admin website has its own MariaDB database; that's
unrelated to how Dovecot itself authenticates.

> **This project is completely vibe-coded** — written by an AI coding agent
> from natural-language instructions, with human review and testing but no
> hand-written code. Read it before you trust it with a production host.

## Contents
This repository contains:
- an installation script that installs and configures everything needed to run the app (Tomcat, MariaDB, the `doveadm` CLI, and the webapp itself)
- the web interface for managing web logins, Dovecot mail accounts, and the link between them

## Requirements
- Debian 13 (Trixie), may also work on Ubuntu
- root access
- the one mail server this app manages, reachable separately — this app manages account *records*; syncing them down to an actual running Dovecot instance is a deferred phase (see PLAN.md)

## What it uses
- Tomcat 10 — Servlet/JSP container
- MariaDB — this app's own database (web logins, mail account records, audit log)
- `doveadm` (from `dovecot-core`) — generates Dovecot-compatible password hashes; the Dovecot *service* itself is stopped and disabled on this host, only the CLI tool is used
- bcrypt (`jbcrypt`) — hashes the website's own login passwords
- Sentry — optional error reporting (vendored jar; no apt package exists for it)

## How to install
Switch to the root account:
> su - root

Move to a folder where you will keep the application permanently, e.g.:
> mkdir -p /opt/git/
> cd /opt/git/

Clone the repository, then run the installation script (can be safely re-run):
> ./install/install.sh

Already-completed steps are skipped on rerun. If a step's script itself changed (e.g. after
`git pull`) and you need it to apply on an existing install, force just that step instead of
reinstalling everything:
> ./install/install.sh --force 13

Or force every step to run again:
> ./install/install.sh --force all

Once the installer finishes, visit `http://<this-host>/javaDovecotAdmin/` to run the first-time
setup wizard — it walks through choosing a database, creating it, and creating the first admin
account.

## Server configuration
Database, RabbitMQ, and Sentry settings all live in `/etc/mailadmin/config.properties` on the
host, not in the database. The admin "Server configuration" page shows all of it; only the
RabbitMQ section is editable from the UI (it writes back to that same file) — the app
deliberately can't rewrite its own database credentials from inside a page that needs those
same credentials to even load.

## Roles
- **ADMIN** — manage web logins, Dovecot mail accounts, the link between them, server config, and the audit log
- **USER** — view and self-service manage (change password, enable/disable) only the Dovecot account(s) an admin has linked to their login

## Recommendation
It is recommended to enable automatic OS updates on this host, preferably via
unattended-upgrades. All installed packages come from the standard Debian repository, with
one deliberate exception (the vendored Sentry SDK jar, since no apt package for it exists).
