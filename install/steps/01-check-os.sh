#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Checking OS..."
[ -f /etc/debian_version ] || error "This installer only supports Debian"
VERSION_ID=$(. /etc/os-release && echo "$VERSION_ID")
[ "$VERSION_ID" = "13" ] || warn "Tested on Debian 13 (Trixie) — found version ${VERSION_ID}, proceeding anyway"
log "OS check passed ($(. /etc/os-release && echo "$PRETTY_NAME"))"

complete_step
