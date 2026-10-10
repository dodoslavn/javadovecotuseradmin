#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Installing dovecot-core (for the doveadm CLI only — this host doesn't run a mail server)..."
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq dovecot-core || error "Failed to install dovecot-core"
which doveadm >/dev/null || error "doveadm binary not found after install"

# The package's postinst starts and enables the actual Dovecot service — we only need the
# doveadm binary to generate password hashes, not a running IMAP/POP3 server on this host.
systemctl stop dovecot >/dev/null 2>&1 || true
systemctl disable dovecot >/dev/null 2>&1 || true
log "doveadm available; dovecot service stopped and disabled"

complete_step
