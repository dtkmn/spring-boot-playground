#!/usr/bin/env bash
set -euo pipefail
BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-localhost:9092}"
cub kafka-ready -b "${BOOTSTRAP_SERVER}" 1 20
kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" --topic basic-messages --replication-factor 1 --partitions 3 --create --if-not-exists
