#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"

echo "Waiting for Kafka to come online..."
cub kafka-ready -b "${BOOTSTRAP_SERVER}" 1 20

echo "Creating Kafka topics..."
create_topic() {
  local topic="$1"
  kafka-topics \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --topic "${topic}" \
    --replication-factor 1 \
    --partitions 4 \
    --create \
    --if-not-exists
}

create_topic "tweets"
create_topic "formatted-tweets"
create_topic "users"
create_topic "crypto-symbols"
create_topic "crypto-prices"
create_topic "moving-average-topic"
create_topic "counts-tweets"

echo "Pre-populating Kafka topics..."
kafka-console-producer \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic tweets \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/inputs.txt

kafka-console-producer \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic users \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/users.txt

kafka-console-producer \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic crypto-symbols \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/crypto-symbols.txt

echo "Kafka bootstrap completed."
