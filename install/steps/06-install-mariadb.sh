#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Installing MariaDB server..."
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq mariadb-server || error "Failed to install mariadb-server"
systemctl is-active mariadb >/dev/null 2>&1 || error "mariadb service not active after install"
log "MariaDB installed and running"

complete_step
