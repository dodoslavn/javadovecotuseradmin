#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Checking for root privileges..."
[ "$EUID" -eq 0 ] || error "Must run as root"
log "Running as root"

complete_step
