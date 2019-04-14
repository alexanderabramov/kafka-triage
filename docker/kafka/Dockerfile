FROM openjdk:11-jre-slim

ARG KAFKA_VERSION=2.2.0
ARG SCALA_VERSION=2.12

ENV KAFKA_DIST=kafka_${SCALA_VERSION}-${KAFKA_VERSION} \
    KAFKA_HOME=/opt/kafka \
    PATH=$PATH:/opt/kafka/bin

# Install Kafka
RUN set -x \
    && apt-get update \
    && apt-get install -y \
        dos2unix \
        gnupg \
        wget \
    && cd /tmp/ \
    && wget -q "http://archive.apache.org/dist/kafka/$KAFKA_VERSION/$KAFKA_DIST.tgz" \
    && wget -q "https://archive.apache.org/dist/kafka/$KAFKA_VERSION/$KAFKA_DIST.tgz.asc" \
    && wget -q "https://kafka.apache.org/KEYS" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --no-tty --import KEYS \
    && gpg --no-tty --batch --verify "$KAFKA_DIST.tgz.asc" "$KAFKA_DIST.tgz" \
    && tar -xzf "$KAFKA_DIST.tgz" -C /opt \
    && rm -r "$GNUPGHOME" "$KAFKA_DIST.tgz" "$KAFKA_DIST.tgz.asc" "KEYS" \
    && cd /opt/$KAFKA_DIST/libs/ \
    && rm *-test.* *-test-sources.* connect-*.jar kafka-streams-*.jar rocksdbjni-*.jar \
    && ln -s /opt/$KAFKA_DIST $KAFKA_HOME \
    && apt-get autoremove -y --purge \
        gnupg \
        wget \
    && rm -rf /var/lib/apt/lists/*

COPY docker-entrypoint.sh /usr/bin/
RUN dos2unix /usr/bin/docker-entrypoint.sh
ENTRYPOINT ["docker-entrypoint.sh"]

CMD ["kafka"]
