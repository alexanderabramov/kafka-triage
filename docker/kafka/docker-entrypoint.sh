#!/bin/sh
set -e

if [ "$1" = 'kafka' ]; then
    exec kafka-server-start.sh $KAFKA_HOME/config/server.properties \
      --override advertised.listeners=PLAINTEXT://${EXTERNAL_IP}:9092 \
      --override zookeeper.connect=zookeeper:2181
fi

exec "$@"
