#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Installing apt-packaged Java libraries..."
# Only what the webapp actually imports — see website/build.sh classpath.
# librabbitmq-client-java pulls in libslf4j-java as a system dependency, but we
# deliberately don't copy ITS jar into WEB-INF/lib — we already vendor a newer
# slf4j-api alongside Sentry, and having two different major versions on the same
# webapp classpath risks NoSuchMethodError. See step 12.
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    libjakarta-servlet-api-java \
    libmariadb-java \
    libpostgresql-jdbc-java \
    libjbcrypt-java \
    librabbitmq-client-java \
    || error "Failed to install Java libraries"
# libpostgresql-jdbc-java is for the optional PostgreSQL backend; the MariaDB driver also serves
# MySQL (jdbc:mariadb://). Both drivers are bundled so the chosen DB system just works — see DbSystem.
log "Java libraries installed (servlet-api, mariadb-java, postgresql-jdbc, jbcrypt, rabbitmq-client)"

complete_step
