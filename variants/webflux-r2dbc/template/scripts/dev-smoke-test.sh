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

APP_HOST_PORT=${APP_HOST_PORT:-__APP_PORT__}
MANAGEMENT_HOST_PORT=${MANAGEMENT_HOST_PORT:-__MANAGEMENT_PORT__}
health_file=$(mktemp)
customers_file=$(mktemp)
boot_log=$(mktemp)
boot_pid=""

cleanup() {
  if [[ -n "$boot_pid" ]] && kill -0 "$boot_pid" 2>/dev/null; then
    kill "$boot_pid" >/dev/null 2>&1 || true
    wait "$boot_pid" >/dev/null 2>&1 || true
  fi
  docker compose -f compose.yaml --env-file .env down -v --remove-orphans >/dev/null 2>&1 || true
  rm -f "$health_file" "$customers_file" "$boot_log"
}
trap cleanup EXIT

docker compose -f compose.yaml --env-file .env down -v --remove-orphans >/dev/null 2>&1 || true

SERVER_PORT="$APP_HOST_PORT" MANAGEMENT_SERVER_PORT="$MANAGEMENT_HOST_PORT" ./gradlew bootRun --no-daemon >"$boot_log" 2>&1 &
boot_pid=$!

for _ in {1..30}; do
  if curl -fsS "http://localhost:${MANAGEMENT_HOST_PORT}/actuator/health" >"$health_file"; then
    if grep -q '"status":"UP"' "$health_file"; then
      break
    fi
  fi

  if ! kill -0 "$boot_pid" 2>/dev/null; then
    cat "$boot_log"
    exit 1
  fi

  sleep 2
done

curl -fsS "http://localhost:${MANAGEMENT_HOST_PORT}/actuator/health" >"$health_file"
grep -q '"status":"UP"' "$health_file"

curl -fsS "http://localhost:${APP_HOST_PORT}/api/v1/customers" >"$customers_file"
grep -q '"firstName":"John"' "$customers_file"
