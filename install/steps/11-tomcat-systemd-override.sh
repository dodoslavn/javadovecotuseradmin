#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Granting Tomcat's systemd unit write access to /etc/mailadmin..."
# tomcat10.service ships with ProtectSystem=strict, which makes /etc read-only to the
# process regardless of Unix file permissions. The RabbitMQ section of Server configuration
# needs to write /etc/mailadmin/config.properties, so it needs an explicit exemption.
mkdir -p /etc/systemd/system/tomcat10.service.d
cat > /etc/systemd/system/tomcat10.service.d/override.conf <<'EOF'
[Service]
ReadWritePaths=/etc/mailadmin
EOF
systemctl daemon-reload
log "systemd override written and reloaded"

complete_step
