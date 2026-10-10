#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Creating /etc/mailadmin (holds config.properties — DB, RabbitMQ, Sentry settings)..."
mkdir -p /etc/mailadmin
mkdir -p /var/lib/mailadmin
chown root:tomcat /etc/mailadmin
chmod 2770 /etc/mailadmin
log "/etc/mailadmin ready (root:tomcat, 2770 — Tomcat can read and, for the RabbitMQ section, write it)"

complete_step
