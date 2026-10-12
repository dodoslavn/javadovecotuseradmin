#!/bin/bash
# tomcatdovecotuseradmin installer orchestrator
# Runs all steps in order. Each step is idempotent — safe to re-run.
# On failure: fix the issue, re-run this script. Completed steps are skipped.
#
# To force specific already-completed steps to run again (e.g. after a step's
# script was updated in a later git pull):
#   sudo bash install.sh --force 10,11        # by number/prefix
#   sudo bash install.sh --force 10-tomcat-port-80
#   sudo bash install.sh --force all          # clear every marker, full re-run

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STEPS_DIR="${SCRIPT_DIR}/steps"
STAMP_DIR="/var/lib/mailadmin/.install"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  tomcatdovecotuseradmin installer${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Error: This installer must be run as root.${NC}"
    echo "Run: sudo bash install.sh"
    exit 1
fi

steps=("$STEPS_DIR"/[0-9]*.sh)

if [ ${#steps[@]} -eq 0 ]; then
    echo -e "${RED}No step scripts found in ${STEPS_DIR}${NC}"
    exit 1
fi

# --force STEP[,STEP...]|all — clear markers so those steps run again
# even though they already completed. Everything else still skips as usual.
if [ "${1:-}" = "--force" ]; then
    force="${2:-}"
    [ -n "$force" ] || { echo -e "${RED}--force needs a step list or 'all'${NC}"; exit 1; }
    mkdir -p "$STAMP_DIR"

    if [ "$force" = "all" ]; then
        rm -f "${STAMP_DIR}"/*.done
        echo -e "${YELLOW}Cleared all step markers — every step will run again.${NC}"
    else
        IFS=',' read -ra patterns <<< "$force"
        for pattern in "${patterns[@]}"; do
            matched=0
            for step in "${steps[@]}"; do
                name=$(basename "$step" .sh)
                if [ "$name" = "$pattern" ] || [[ "$name" == "${pattern}"-* ]]; then
                    rm -f "${STAMP_DIR}/${name}.done"
                    echo -e "${YELLOW}Cleared marker for ${name} — will run again.${NC}"
                    matched=1
                fi
            done
            [ "$matched" -eq 1 ] || { echo -e "${RED}No step matches '${pattern}'${NC}"; exit 1; }
        done
    fi
    echo ""
fi

total=${#steps[@]}
current=0

for step in "${steps[@]}"; do
    current=$((current + 1))
    name=$(basename "$step" .sh)
    echo -e "${BLUE}[${current}/${total}]${NC} ${name}"
    bash "$step" || {
        echo ""
        echo -e "${RED}======================================${NC}"
        echo -e "${RED}  Installation failed at: ${name}${NC}"
        echo -e "${RED}  Fix the issue and re-run install.sh${NC}"
        echo -e "${RED}  Completed steps will be skipped.${NC}"
        echo -e "${RED}======================================${NC}"
        exit 1
    }
done

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Installation complete!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "Visit http://<this-host>/tomcatdovecotuseradmin/ to run the first-time setup wizard"
echo "(choose a database, create it, and create the first admin account)."
echo ""
