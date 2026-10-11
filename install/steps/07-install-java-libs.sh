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
    libjbcrypt-java \
    librabbitmq-client-java \
    || error "Failed to install Java libraries"
log "Java libraries installed (servlet-api, mariadb-java, jbcrypt, rabbitmq-client)"

complete_step
