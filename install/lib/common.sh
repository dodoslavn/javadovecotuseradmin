#!/bin/bash
# Shared helpers for all install steps

STAMP_DIR="/var/lib/mailadmin/.install"
LOG_FILE="/var/log/mailadmin-install.log"

# Ensure log file is writable even before the config-dir step creates its directory
mkdir -p /var/log 2>/dev/null || true

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    local msg="[$(date '+%H:%M:%S')] $*"
    echo -e "${GREEN}[OK]${NC} $*"
    echo "$msg" >> "$LOG_FILE"
}

info() {
    local msg="[$(date '+%H:%M:%S')] INFO: $*"
    echo -e "${BLUE}[..] $*${NC}"
    echo "$msg" >> "$LOG_FILE"
}

warn() {
    local msg="[$(date '+%H:%M:%S')] WARN: $*"
    echo -e "${YELLOW}[WARN]${NC} $*"
    echo "$msg" >> "$LOG_FILE"
}

error() {
    local msg="[$(date '+%H:%M:%S')] FAIL: $*"
    echo -e "${RED}[FAIL]${NC} $*" >&2
    echo "$msg" >> "$LOG_FILE"
    exit 1
}

check_done() {
    local step="$1"
    [ -f "${STAMP_DIR}/${step}.done" ]
}

mark_done() {
    local step="$1"
    mkdir -p "$STAMP_DIR"
    touch "${STAMP_DIR}/${step}.done"
}

step_name() {
    basename "$0" .sh
}

skip_if_done() {
    local step
    step=$(step_name)
    if check_done "$step"; then
        echo -e "${YELLOW}[SKIP]${NC} $(step_name) — already completed"
        exit 0
    fi
}

complete_step() {
    local step
    step=$(step_name)
    mark_done "$step"
    log "$(step_name) completed"
}
