#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"

cub kafka-ready -b "${BOOTSTRAP_SERVER}" 1 20

create_topic() {
  kafka-topics \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --topic "$1" \
    --replication-factor 1 \
    --partitions 4 \
    --create \
    --if-not-exists
}

create_topic "tweets"
create_topic "formatted-tweets"
create_topic "crypto-symbols"
create_topic "crypto-prices"
create_topic "moving-average-topic"
create_topic "counts-tweets"
create_topic "reddit-posts"
create_topic "alerts"

kafka-console-producer \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic tweets \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/inputs.txt

kafka-console-producer \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic crypto-symbols \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/crypto-symbols.txt

kafka-console-producer \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic reddit-posts \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/reddit-posts.txt
