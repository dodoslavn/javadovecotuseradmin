#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Installing Tomcat 10..."
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq tomcat10 || error "Failed to install tomcat10"
systemctl is-enabled tomcat10 >/dev/null 2>&1 || systemctl enable tomcat10 >/dev/null
log "Tomcat 10 installed"

complete_step
