#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Running apt-get update..."
apt-get update -qq || error "apt-get update failed"
log "apt package lists updated"

complete_step
