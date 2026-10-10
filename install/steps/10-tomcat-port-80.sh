#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Moving Tomcat's connector from 8080 to 80, so it's reachable without a reverse proxy..."

JAVA_BIN="$(readlink -f "$(command -v java)")"
if ! getcap "$JAVA_BIN" | grep -q cap_net_bind_service; then
    setcap 'cap_net_bind_service=+ep' "$JAVA_BIN" || error "setcap failed on $JAVA_BIN"
fi
log "cap_net_bind_service granted to $JAVA_BIN (lets Tomcat, running as the non-root 'tomcat' user, bind port 80)"

SERVER_XML="/etc/tomcat10/server.xml"
if grep -q 'Connector port="8080"' "$SERVER_XML"; then
    sed -i 's/Connector port="8080"/Connector port="80"/' "$SERVER_XML" || error "Failed to edit $SERVER_XML"
    log "server.xml connector changed to port 80"
else
    info "server.xml already on a different port — leaving it alone"
fi

complete_step
