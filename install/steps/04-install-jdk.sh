#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Installing JDK..."
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq default-jdk-headless || error "Failed to install JDK"
javac -version >/dev/null 2>&1 || error "javac not working after install"
log "JDK installed: $(java -version 2>&1 | head -1)"

complete_step
