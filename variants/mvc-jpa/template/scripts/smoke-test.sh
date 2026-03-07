#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$script_dir/.." && pwd)
cd "$repo_root"

if [[ ! -f .env ]]; then
  cp .env.example .env
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

APP_HOST_PORT=${APP_HOST_PORT:-8080}
MANAGEMENT_HOST_PORT=${MANAGEMENT_HOST_PORT:-8081}
health_file=$(mktemp)
customers_file=$(mktemp)

cleanup() {
  docker compose --env-file .env down -v --remove-orphans >/dev/null 2>&1 || true
  rm -f "$health_file" "$customers_file"
}
trap cleanup EXIT

docker compose --env-file .env up -d --build

for _ in {1..30}; do
  if curl -fsS "http://localhost:${MANAGEMENT_HOST_PORT}/actuator/health" >"$health_file"; then
    if grep -q '"status":"UP"' "$health_file"; then
      break
    fi
  fi
  sleep 2
done

curl -fsS "http://localhost:${MANAGEMENT_HOST_PORT}/actuator/health" >"$health_file"
grep -q '"status":"UP"' "$health_file"

curl -fsS "http://localhost:${APP_HOST_PORT}/api/v1/customers" >"$customers_file"
grep -q '"firstName":"John"' "$customers_file"
