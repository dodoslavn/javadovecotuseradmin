#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Checking internet connectivity..."
curl -s -m 5 -o /dev/null -w "" https://deb.debian.org/ || error "No internet access — required for apt and fetching the Sentry SDK jar"
log "Internet connectivity OK"

complete_step
