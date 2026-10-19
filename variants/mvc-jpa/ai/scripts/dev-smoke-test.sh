#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$script_dir/.." && pwd)
cd "$repo_root"

[[ -f .env ]] || { echo "Create private .env credentials first; see README.md" >&2; exit 1; }
set -a
# shellcheck disable=SC1091
source .env
set +a
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${PILOT_CALLER_A_TOKEN:?PILOT_CALLER_A_TOKEN is required}"
: "${PILOT_CALLER_B_TOKEN:?PILOT_CALLER_B_TOKEN is required}"

APP_HOST_PORT=${APP_HOST_PORT:-__APP_PORT__}
MANAGEMENT_HOST_PORT=${MANAGEMENT_HOST_PORT:-__MANAGEMENT_PORT__}
export POSTGRES_EXPORT_PORT=${POSTGRES_EXPORT_PORT:-15432}
compose=(docker compose -p __ARTIFACT_ID__-development-smoke -f compose.yaml --env-file .env)
response_file=$(mktemp)
boot_log=$(mktemp)
boot_pid=""

cleanup() {
  if [[ -n "$boot_pid" ]] && kill -0 "$boot_pid" 2>/dev/null; then
    kill "$boot_pid" >/dev/null 2>&1 || true
    wait "$boot_pid" >/dev/null 2>&1 || true
  fi
  "${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
  rm -f "$response_file" "$boot_log"
}
trap cleanup EXIT

authenticated_curl() {
  local token=$1
  shift
  printf 'header = "Authorization: Bearer %s"\n' "$token" | curl --config - "$@"
}

"${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
"${compose[@]}" up -d --wait
# Regular smoke proves the secured service can run with AI off and no Ollama process.
SPRING_DOCKER_COMPOSE_ENABLED=false SPRING_PROFILES_ACTIVE=default \
  POSTGRES_HOST=127.0.0.1 POSTGRES_PORT="$POSTGRES_EXPORT_PORT" \
  SERVER_PORT="$APP_HOST_PORT" MANAGEMENT_SERVER_PORT="$MANAGEMENT_HOST_PORT" \
  ./gradlew bootRun --no-daemon >"$boot_log" 2>&1 &
boot_pid=$!

for _ in {1..60}; do
  if curl -fsS "http://127.0.0.1:${MANAGEMENT_HOST_PORT}/actuator/health" >"$response_file" 2>/dev/null &&
     grep -q '"status":"UP"' "$response_file"; then
    break
  fi
  if ! kill -0 "$boot_pid" 2>/dev/null; then
    echo "bootRun stopped before startup; inspect application startup locally." >&2
    exit 1
  fi
  sleep 2
done

curl -fsS "http://127.0.0.1:${MANAGEMENT_HOST_PORT}/actuator/health" >"$response_file"
grep -q '"status":"UP"' "$response_file"
! grep -Eq '"(components|details)"' "$response_file"
for probe in liveness readiness; do
  curl -fsS "http://127.0.0.1:${MANAGEMENT_HOST_PORT}/actuator/health/$probe" >"$response_file"
  grep -q '"status":"UP"' "$response_file"
done
status=$(curl -sS -o "$response_file" -w '%{http_code}' "http://127.0.0.1:${APP_HOST_PORT}/api/v1/orders/1001")
[[ "$status" == 401 ]]
authenticated_curl "$PILOT_CALLER_A_TOKEN" -fsS "http://127.0.0.1:${APP_HOST_PORT}/api/v1/orders/1001" >"$response_file"
grep -q '"id":1001' "$response_file"
grep -q '"status":"DELIVERED"' "$response_file"
status=$(authenticated_curl "$PILOT_CALLER_A_TOKEN" -sS -o "$response_file" -w '%{http_code}' "http://127.0.0.1:${APP_HOST_PORT}/api/v1/orders/2001")
[[ "$status" == 404 ]]
grep -q 'Order not accessible' "$response_file"
! grep -q 'Private B' "$response_file"
authenticated_curl "$PILOT_CALLER_B_TOKEN" -fsS "http://127.0.0.1:${APP_HOST_PORT}/api/v1/orders/2001" >"$response_file"
grep -q '"id":2001' "$response_file"
status=$(curl -sS -o "$response_file" -w '%{http_code}' "http://127.0.0.1:${MANAGEMENT_HOST_PORT}/actuator/prometheus")
[[ "$status" == 401 ]]
authenticated_curl "$PILOT_CALLER_A_TOKEN" -fsS "http://127.0.0.1:${MANAGEMENT_HOST_PORT}/actuator/prometheus" >"$response_file"
grep -q 'jvm_' "$response_file"
status=$(authenticated_curl "$PILOT_CALLER_A_TOKEN" -sS -o "$response_file" -w '%{http_code}' \
  -H 'Content-Type: application/json' -d '{"orderId":1002,"question":"status"}' \
  "http://127.0.0.1:${APP_HOST_PORT}/api/ai/order-support")
[[ "$status" == 503 ]]
grep -q 'Assistant unavailable' "$response_file"
status=$(authenticated_curl "$PILOT_CALLER_A_TOKEN" -sS -o "$response_file" -w '%{http_code}' \
  "http://127.0.0.1:${APP_HOST_PORT}/not-a-route")
[[ "$status" == 404 ]]
grep -q 'Resource not found' "$response_file"
echo "Development Order API, caller isolation, health, metrics and AI-off startup passed."
