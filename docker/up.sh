#!/bin/sh
set -e
exists() {
    command -v $1 > /dev/null 2>&1
}

exists docker || { echo "Cannot find docker (https://docs.docker.com/install/ or https://docs.docker.com/toolbox/toolbox_install_windows/)"; exit 1; }
exists docker-compose || { echo "Cannot find docker-compose (https://docs.docker.com/compose/install/)"; exit 1; }

EXTERNAL_IP=$(docker-machine ip default) exec docker-compose up --build
