#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

SENTRY_VERSION="7.14.0"
SLF4J_VERSION="2.0.13"
LIB_DIR="$(dirname "$0")/../../website/src/main/webapp/WEB-INF/lib"

info "Populating WEB-INF/lib with runtime dependencies..."
mkdir -p "$LIB_DIR"

# Apt-provided jars: build.sh's classpath globs *.jar already sitting in WEB-INF/lib, it
# doesn't copy these itself — they need to already be here before the first build.
cp -n /usr/share/java/mariadb-java-client.jar "$LIB_DIR/" 2>/dev/null || true
cp -n /usr/share/java/jbcrypt.jar "$LIB_DIR/" 2>/dev/null || true
cp -n /usr/share/java/amqp-client.jar "$LIB_DIR/" 2>/dev/null || true
[ -f "${LIB_DIR}/mariadb-java-client.jar" ] || error "mariadb-java-client.jar missing — did step 07 run?"
[ -f "${LIB_DIR}/jbcrypt.jar" ] || error "jbcrypt.jar missing — did step 07 run?"
[ -f "${LIB_DIR}/amqp-client.jar" ] || error "amqp-client.jar missing — did step 07 run?"
# Deliberately NOT copying /usr/share/java/slf4j-api*.jar here — amqp-client depends on it
# at the OS package level, but we already vendor a newer slf4j-api below for Sentry, and
# amqp-client works fine against it (slf4j kept the core Logger API compatible across
# 1.x -> 2.x). Two different slf4j-api major versions on the same classpath risks
# NoSuchMethodError, so there must be exactly one.

info "Vendoring the Sentry Java SDK (no apt package exists for it — the one deliberate exception to apt-only)..."
if [ ! -f "${LIB_DIR}/sentry-${SENTRY_VERSION}.jar" ]; then
    curl -sL -o "${LIB_DIR}/sentry-${SENTRY_VERSION}.jar" \
        "https://repo1.maven.org/maven2/io/sentry/sentry/${SENTRY_VERSION}/sentry-${SENTRY_VERSION}.jar" \
        || error "Failed to download sentry-java"
fi
if [ ! -f "${LIB_DIR}/slf4j-api-${SLF4J_VERSION}.jar" ]; then
    curl -sL -o "${LIB_DIR}/slf4j-api-${SLF4J_VERSION}.jar" \
        "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/${SLF4J_VERSION}/slf4j-api-${SLF4J_VERSION}.jar" \
        || error "Failed to download slf4j-api"
fi
log "sentry-java ${SENTRY_VERSION}, slf4j-api ${SLF4J_VERSION}, and amqp-client present in WEB-INF/lib"

complete_step
