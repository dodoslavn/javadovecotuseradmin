#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

WEBSITE_DIR="$(dirname "$0")/../../website"

info "Building the WAR..."
bash "${WEBSITE_DIR}/build.sh" || error "build.sh failed"

info "Deploying to Tomcat..."
rm -rf /var/lib/tomcat10/webapps/javaDovecotAdmin /var/lib/tomcat10/webapps/javaDovecotAdmin.war
cp "${WEBSITE_DIR}/javaDovecotAdmin.war" /var/lib/tomcat10/webapps/javaDovecotAdmin.war || error "Failed to copy WAR to Tomcat webapps"

systemctl is-active tomcat10 >/dev/null 2>&1 || systemctl start tomcat10
log "javaDovecotAdmin.war deployed"

complete_step
