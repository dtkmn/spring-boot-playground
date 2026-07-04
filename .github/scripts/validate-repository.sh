#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

required_paths=(
  "variants/mvc-jpa"
  "variants/mvc-jpa/template/.gitignore"
  "variants/mvc-jpa/template/.dockerignore"
  "variants/mvc-jpa/template/compose.yaml"
  "variants/mvc-jpa/template/README.md"
  "variants/mvc-jpa/template/.github/workflows/ci.yml"
  "variants/mvc-jpa/template/.github/workflows/publish.yml"
  "variants/mvc-jpa/template/.github/dependabot.yaml"
  "variants/mvc-jpa/template/SUPPORT.md"
  "variants/mvc-jpa/template/CHANGELOG.md"
  "variants/mvc-jpa/template/RELEASING.md"
  "variants/mvc-jpa/template/scripts/dev-smoke-test.sh"
  "variants/mvc-jpa/template/scripts/smoke-test.sh"
  "variants/mvc-jpa/template/deploy/helm/values-dev.yaml"
  "variants/mvc-jpa/template/deploy/helm/values-staging.yaml"
  "variants/mvc-jpa/template/deploy/helm/values-prod.yaml"
  "variants/webflux-r2dbc"
  "variants/webflux-r2dbc/template/.gitignore"
  "variants/webflux-r2dbc/template/.dockerignore"
  "variants/webflux-r2dbc/template/compose.yaml"
  "variants/webflux-r2dbc/template/README.md"
  "variants/webflux-r2dbc/template/.github/workflows/ci.yml"
  "variants/webflux-r2dbc/template/.github/workflows/publish.yml"
  "variants/webflux-r2dbc/template/.github/dependabot.yaml"
  "variants/webflux-r2dbc/template/SUPPORT.md"
  "variants/webflux-r2dbc/template/CHANGELOG.md"
  "variants/webflux-r2dbc/template/RELEASING.md"
  "variants/webflux-r2dbc/template/scripts/dev-smoke-test.sh"
  "variants/webflux-r2dbc/template/scripts/smoke-test.sh"
  "variants/webflux-r2dbc/template/deploy/helm/values-dev.yaml"
  "variants/webflux-r2dbc/template/deploy/helm/values-staging.yaml"
  "variants/webflux-r2dbc/template/deploy/helm/values-prod.yaml"
  "examples/kafka-basic"
  "examples/kafka-streams"
  "examples/binance-websocket"
  "deploy/helm/spring-service-starter"
  "deploy/helm/spring-service-starter/values.schema.json"
  ".github/ISSUE_TEMPLATE/pilot-feedback.yml"
  "docs/adr/ADR-001-default-starter-and-variants.md"
  "docs/adr/ADR-002-kubernetes-and-helm.md"
  "docs/adr/ADR-003-2026-modernization-tranches.md"
  "docs/adr/ADR-004-promotion-and-supply-chain-baseline.md"
  "docs/adoption/pilot-playbook.md"
  "docs/adoption/promotion-brief.md"
  "docs/releases/release-readiness-checklist.md"
  "docs/releases/version-policy.md"
  "docs/security/supply-chain-baseline.md"
  "RELEASING.md"
  "scripts/init-service.sh"
)

failures=0

for path in "${required_paths[@]}"; do
  if [[ ! -e "$path" ]]; then
    echo "::error::Missing required repository path: $path"
    failures=1
  fi
done

require_contains() {
  local file="$1"
  local needle="$2"
  local message="$3"

  if ! grep -Fq -- "$needle" "$file"; then
    echo "::error file=$file::$message"
    failures=1
  fi
}

validate_stable_line_dependabot_rules() {
  local file="$1"

  require_contains "$file" 'dependency-name: "org.springframework.boot*"' \
    "Dependabot must ignore Spring Boot major updates"
  require_contains "$file" "spring-boot-minor-patch:" \
    "Dependabot must group Spring Boot minor/patch updates"
  require_contains "$file" 'dependency-name: "eclipse-temurin"' \
    "Dependabot must ignore eclipse-temurin major updates"
  require_contains "$file" "eclipse-temurin-minor-patch:" \
    "Dependabot must group eclipse-temurin minor/patch updates"
  require_contains "$file" '- "version-update:semver-major"' \
    "Dependabot must ignore semver major updates for stable lines"
  require_contains "$file" '- "minor"' \
    "Dependabot stable-line groups must include minor updates"
  require_contains "$file" '- "patch"' \
    "Dependabot stable-line groups must include patch updates"
}

validate_stable_line_dependabot_rules ".github/dependabot.yaml"

for template_path in "variants/mvc-jpa/template" "variants/webflux-r2dbc/template"; do
  require_contains "$template_path/build.gradle" "id 'org.cyclonedx.bom'" \
    "Missing CycloneDX SBOM plugin"
  require_contains "$template_path/build.gradle" "id 'jacoco'" \
    "Missing JaCoCo coverage plugin"
  require_contains "$template_path/Dockerfile" "USER 10001:10001" \
    "Dockerfile must run as a non-root user"
  require_contains "$template_path/.github/workflows/ci.yml" "./gradlew check --no-daemon" \
    "CI must run Gradle check"
  require_contains "$template_path/.github/workflows/publish.yml" "./gradlew check --no-daemon" \
    "Publish workflow must run Gradle check"
  require_contains "$template_path/.github/dependabot.yaml" 'package-ecosystem: "docker"' \
    "Dependabot must track Docker updates"
  validate_stable_line_dependabot_rules "$template_path/.github/dependabot.yaml"
done

if [[ "$failures" -ne 0 ]]; then
  exit 1
fi

echo "Repository layout and starter contracts are valid."
