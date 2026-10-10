#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Verifying installation..."

systemctl is-active --quiet tomcat10 || error "tomcat10 is not active"
log "tomcat10 active"

systemctl is-active --quiet mariadb || error "mariadb is not active"
log "mariadb active"

which doveadm >/dev/null || error "doveadm not found"
log "doveadm available"

ss -tln | grep -q ':80 ' || error "nothing listening on port 80"
log "port 80 listening"

code=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/javaDovecotAdmin/ || true)
[ "$code" = "302" ] || [ "$code" = "200" ] || error "app did not respond as expected on port 80 (got HTTP ${code})"
log "app responding on http://127.0.0.1/javaDovecotAdmin/ (HTTP ${code})"

echo ""
log "All checks passed."

complete_step
