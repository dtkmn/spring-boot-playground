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
compose=(docker compose -p __ARTIFACT_ID__-container-smoke -f docker-compose.yml --env-file .env)
response_file=$(mktemp)

cleanup() {
  "${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
  rm -f "$response_file"
}
trap cleanup EXIT

# Credentials go through stdin, without printed output or curl's process arguments.
authenticated_curl() {
  local token=$1
  shift
  printf 'header = "Authorization: Bearer %s"\n' "$token" | curl --config - "$@"
}

"${compose[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
PILOT_PROFILES=docker "${compose[@]}" up -d --build

for _ in {1..60}; do
  if curl -fsS "http://127.0.0.1:${MANAGEMENT_HOST_PORT}/actuator/health" >"$response_file" 2>/dev/null &&
     grep -q '"status":"UP"' "$response_file"; then
    break
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
echo "Packaged Order API, caller isolation, health, metrics and AI-off startup passed."
