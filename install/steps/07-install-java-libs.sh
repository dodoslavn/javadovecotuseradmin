#!/bin/bash
source "$(dirname "$0")/../lib/common.sh"
skip_if_done

info "Installing apt-packaged Java libraries..."
# Only what the webapp actually imports — see website/build.sh classpath.
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    libjakarta-servlet-api-java \
    libmariadb-java \
    libjbcrypt-java \
    || error "Failed to install Java libraries"
log "Java libraries installed (servlet-api, mariadb-java, jbcrypt)"

complete_step
