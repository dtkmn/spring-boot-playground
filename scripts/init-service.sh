#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<USAGE
Usage:
  ./scripts/init-service.sh \\
    --variant mvc-jpa|webflux-r2dbc \\
    [--purpose api|ai] \\
    --service-name <name> \\
    --group-id <group> \\
    --artifact-id <artifact> \\
    --package-name <package> \\
    [--output-dir <dir>] \\
    [--app-port <port>] \\
    [--management-port <port>]

Purpose defaults to api. Experimental ai requires mvc-jpa and uses Ollama.
USAGE
}
fail() { echo "$*" >&2; exit 1; }

variant=""
purpose="api"
service_name=""
group_id=""
artifact_id=""
package_name=""
output_dir=""
app_port="8080"
management_port="8081"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --variant|--purpose|--service-name|--group-id|--artifact-id|--package-name|--output-dir|--app-port|--management-port)
      [[ $# -ge 2 && -n "$2" && "$2" != --* ]] || fail "Missing value for $1"
      option=$1
      value=$2
      case "$option" in
        --variant) variant=$value ;;
        --purpose) purpose=$value ;;
        --service-name) service_name=$value ;;
        --group-id) group_id=$value ;;
        --artifact-id) artifact_id=$value ;;
        --package-name) package_name=$value ;;
        --output-dir) output_dir=$value ;;
        --app-port) app_port=$value ;;
        --management-port) management_port=$value ;;
      esac
      shift 2
      ;;
    *) fail "Unknown argument: $1" ;;
  esac
done

[[ -n "$variant" && -n "$service_name" && -n "$group_id" && -n "$artifact_id" && -n "$package_name" ]] ||
  { usage >&2; fail "Variant, service name, group, artifact and package are required"; }
case "$variant" in mvc-jpa|webflux-r2dbc) ;; *) fail "Unsupported variant: $variant" ;; esac
case "$purpose" in api|ai) ;; *) fail "Unsupported purpose: $purpose" ;; esac
[[ "$purpose" != ai || "$variant" == mvc-jpa ]] || fail "AI purpose requires the mvc-jpa variant"

for name in "$service_name" "$artifact_id"; do
  [[ "$name" =~ ^[a-z0-9]+(-[a-z0-9]+)*$ && ${#name} -le 63 ]] ||
    fail "Service and artifact names must be lowercase letters/digits separated by hyphens, at most 63 characters"
done
[[ "$group_id" =~ ^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$ ]] ||
  fail "Group ID must contain dot-separated identifiers"
[[ "$package_name" =~ ^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$ ]] ||
  fail "Package must contain dot-separated Java identifiers"
IFS='.' read -r -a package_parts <<< "$package_name"
for part in "${package_parts[@]}"; do
  case "$part" in
    abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false|null|_)
      fail "Package contains a reserved Java identifier: $part" ;;
  esac
done
for port in "$app_port" "$management_port"; do
  [[ "$port" =~ ^[0-9]+$ && ${#port} -le 5 ]] || fail "Ports must be integers from 1 to 65535"
  (( 10#$port >= 1 && 10#$port <= 65535 )) || fail "Ports must be integers from 1 to 65535"
done
app_port=$((10#$app_port))
management_port=$((10#$management_port))
[[ "$app_port" != "$management_port" ]] || fail "Application and management ports must differ"

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$script_dir/.." && pwd -P)
template_dir="$repo_root/variants/$variant/template"
ai_dir="$repo_root/variants/mvc-jpa/ai"
shared_chart_dir="$repo_root/deploy/helm/spring-service-starter"
output_dir=${output_dir:-"$repo_root/generated/$artifact_id"}
package_path=${package_name//./\/}

# Resolve the existing ancestor without creating anything. Refuse a dangling
# link, a file parent or a destination inside any source assets.
[[ ! -e "$output_dir" && ! -L "$output_dir" ]] || fail "Output directory already exists: $output_dir"
[[ "$output_dir" == /* ]] || output_dir="$PWD/$output_dir"
while [[ "$output_dir" == */ ]]; do output_dir=${output_dir%/}; done
leaf=${output_dir##*/}
[[ -n "$leaf" && "$leaf" != . && "$leaf" != .. ]] || fail "Choose a new output directory"
parent=${output_dir%/*}
suffix="/$leaf"
while [[ ! -d "$parent" ]]; do
  [[ ! -e "$parent" && ! -L "$parent" ]] || fail "Output parent is not a directory: $parent"
  component=${parent##*/}
  [[ "$component" != . && "$component" != .. && -n "$component" ]] ||
    fail "Normalize output parents before generation"
  suffix="/$component$suffix"
  parent=${parent%/*}
  [[ -n "$parent" ]] || parent="/"
done
parent=$(cd "$parent" && pwd -P)
output_dir="${parent%/}$suffix"
case "$output_dir/" in
  "$repo_root/variants/"*|"$repo_root/deploy/"*|"$repo_root/gradle/"*|"$repo_root/scripts/"*)
    fail "Output must be outside starter source assets" ;;
esac

require_file() { [[ -f "$1" && ! -L "$1" ]] || fail "Required asset missing or not a regular file: $1"; }
common_files=(
  .env.example .gitignore .dockerignore build.gradle settings.gradle Dockerfile
  README.md SUPPORT.md CHANGELOG.md RELEASING.md compose.yaml docker-compose.yml
  .github/workflows/ci.yml .github/workflows/publish.yml .github/dependabot.yaml
  scripts/dev-smoke-test.sh scripts/smoke-test.sh
  src/main/java/__PACKAGE_PATH__/Application.java
  src/main/java/__PACKAGE_PATH__/web/ApiExceptionHandler.java
  src/main/resources/application.yaml src/main/resources/application-docker.yaml
  src/main/resources/application-structured-logging.yaml
  src/test/java/__PACKAGE_PATH__/observability/TracingConfigurationTest.java
)
for file in "${common_files[@]}"; do require_file "$template_dir/$file"; done
for file in gradlew gradlew.bat LICENSE gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties; do
  require_file "$repo_root/$file"
done
customer_files=(
  src/main/java/__PACKAGE_PATH__/domain/Customer.java
  src/main/java/__PACKAGE_PATH__/domain/CustomerRequest.java
  src/main/java/__PACKAGE_PATH__/domain/CustomerResponse.java
  src/main/java/__PACKAGE_PATH__/repository/CustomerRepository.java
  src/main/java/__PACKAGE_PATH__/service/CustomerService.java
  src/main/java/__PACKAGE_PATH__/web/CustomerController.java
  src/test/java/__PACKAGE_PATH__/web/CustomerApiIntegrationTest.java
  src/test/java/__PACKAGE_PATH__/web/CustomerControllerTest.java
  src/main/resources/db/migration/V1__create_customer_table.sql
)
for file in "${customer_files[@]}"; do require_file "$template_dir/$file"; done
if [[ "$purpose" == api ]]; then
  for file in Chart.yaml values.yaml values.schema.json README.md values-dev.yaml values-staging.yaml values-prod.yaml \
      templates/_helpers.tpl templates/deployment.yaml templates/service.yaml templates/serviceaccount.yaml \
      templates/hpa.yaml templates/ingress.yaml templates/pdb.yaml; do
    require_file "$shared_chart_dir/$file"
  done
  for file in values-dev.yaml values-staging.yaml values-prod.yaml; do require_file "$template_dir/deploy/helm/$file"; done
else
  ai_files=(
    .env.example README.md SUPPORT.md CHANGELOG.md RELEASING.md compose.yaml docker-compose.yml gradle/ai.gradle
    .github/workflows/ci.yml .github/workflows/publish.yml scripts/dev-smoke-test.sh scripts/smoke-test.sh
    src/main/resources/application.yaml src/main/resources/application-docker.yaml src/main/resources/application-ai.yaml
    src/main/resources/db/migration/V1__create_order_tables.sql src/main/resources/db/migration/V2__seed_order_fixtures.sql
    src/main/java/__PACKAGE_PATH__/domain/PurchaseOrder.java src/main/java/__PACKAGE_PATH__/domain/Shipment.java
    src/main/java/__PACKAGE_PATH__/repository/OrderRepository.java src/main/java/__PACKAGE_PATH__/repository/ShipmentRepository.java
    src/main/java/__PACKAGE_PATH__/service/OrderService.java src/main/java/__PACKAGE_PATH__/security/LocalBearerSecurityConfiguration.java
    src/main/java/__PACKAGE_PATH__/ai/OrderAssistant.java src/main/java/__PACKAGE_PATH__/ai/InvocationBudget.java
    src/main/java/__PACKAGE_PATH__/ai/OrderTools.java src/main/java/__PACKAGE_PATH__/web/OrderController.java
    src/main/java/__PACKAGE_PATH__/web/OrderSupportController.java src/main/java/__PACKAGE_PATH__/web/ApiExceptionHandler.java
    src/test/java/__PACKAGE_PATH__/OrderAssistantIntegrationTest.java src/test/java/__PACKAGE_PATH__/ScriptedOllama.java
    src/test/java/__PACKAGE_PATH__/OllamaOrderEvaluationTest.java src/test/java/__PACKAGE_PATH__/LocalBearerStartupTest.java
    src/test/java/__PACKAGE_PATH__/ai/InvocationBudgetTest.java
  )
  for file in "${ai_files[@]}"; do require_file "$ai_dir/$file"; done
  docker_anchor_count=0
  while IFS= read -r line; do
    [[ "$line" != 'COPY settings.gradle .' ]] || docker_anchor_count=$((docker_anchor_count + 1))
  done < "$template_dir/Dockerfile"
  [[ "$docker_anchor_count" == 1 ]] || fail "MVC Dockerfile must have exactly one settings.gradle copy step"
fi

# Capture provenance before creating output. Archives do not inherit a containing
# repository's identity, and modified source cannot be reproduced by commit alone.
starter_commit="unknown"
starter_version="unknown"
starter_worktree="unknown"
if [[ "$(git -C "$repo_root" rev-parse --show-toplevel 2>/dev/null || true)" == "$repo_root" ]] \
    && starter_commit=$(git -C "$repo_root" rev-parse --verify HEAD 2>/dev/null); then
  starter_version="unreleased"
  starter_worktree="modified"
  source_changes=$(git -C "$repo_root" status --porcelain --untracked-files=normal)
  if [[ -z "$source_changes" ]]; then
    starter_worktree="clean"
    while IFS= read -r starter_tag; do
      if [[ "$starter_tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then starter_version="$starter_tag"; break; fi
    done < <(git -C "$repo_root" tag --points-at HEAD --list 'v*' --sort=-version:refname)
  fi
else
  starter_commit="unknown"
fi

copy_assets() {
  tar -C "$1" \
    --exclude='.gradle' --exclude='build' --exclude='out' \
    --exclude='.idea' --exclude='*.iml' --exclude='.git' \
    --exclude='.env' --exclude='.env.*' --exclude='.DS_Store' --exclude='*.log' \
    -cf - . | tar -C "$output_dir" -xf -
  cp "$1/.env.example" "$output_dir/.env.example"
}
mkdir -p "$(dirname "$output_dir")"
mkdir "$output_dir"
# Remove only this invocation's new output on an extraction/composition failure.
trap 'rm -rf -- "$output_dir"' ERR
copy_assets "$template_dir"
if [[ "$purpose" == ai ]]; then
  for file in "${customer_files[@]}"; do rm -- "$output_dir/$file"; done
  rm -rf -- "$output_dir/deploy"
  copy_assets "$ai_dir"
  printf "\napply from: 'gradle/ai.gradle'\n" >> "$output_dir/build.gradle"
  printf '!gradle/\n!gradle/ai.gradle\n' >> "$output_dir/.dockerignore"
  docker_content=""
  while IFS= read -r line || [[ -n "$line" ]]; do
    docker_content+="$line"$'\n'
    if [[ "$line" == 'COPY settings.gradle .' ]]; then
      docker_content+='COPY gradle/ai.gradle ./gradle/ai.gradle'$'\n'
    fi
  done < "$output_dir/Dockerfile"
  printf '%s' "$docker_content" > "$output_dir/Dockerfile"
fi
cp "$repo_root/gradlew" "$repo_root/gradlew.bat" "$repo_root/LICENSE" "$output_dir/"
mkdir -p "$output_dir/gradle/wrapper"
cp "$repo_root/gradle/wrapper/gradle-wrapper.jar" "$repo_root/gradle/wrapper/gradle-wrapper.properties" "$output_dir/gradle/wrapper/"
if [[ "$purpose" == api ]]; then
  mkdir -p "$output_dir/deploy/helm"
  cp -R "$shared_chart_dir" "$output_dir/deploy/helm/"
fi

while IFS= read -r -d '' dir; do
  target_dir=$(dirname "$dir")/$package_path
  mkdir -p "$(dirname "$target_dir")"
  mv "$dir" "$target_dir"
done < <(find "$output_dir" -depth -type d -name '__PACKAGE_PATH__' -print0)

while IFS= read -r -d '' file; do
  # Read through EOF including trailing newlines. A NUL marks binary assets.
  if IFS= read -r -d '' content < "$file"; then continue; fi
  original_content=$content
  content=${content//__SERVICE_NAME__/"$service_name"}
  content=${content//__GROUP_ID__/"$group_id"}
  content=${content//__ARTIFACT_ID__/"$artifact_id"}
  content=${content//__PACKAGE_NAME__/"$package_name"}
  content=${content//__APP_PORT__/"$app_port"}
  content=${content//__MANAGEMENT_PORT__/"$management_port"}
  if [[ "$content" != "$original_content" ]]; then printf '%s' "$content" > "$file"; fi
done < <(find "$output_dir" -type f -print0)

model_integration="none"
[[ "$purpose" != ai ]] || model_integration="ollama"
{
  cat <<EOF
# Starter origin

- Upstream repository: https://github.com/dtkmn/spring-boot-playground
- Source commit: $starter_commit
- Source version: $starter_version
- Working tree: $starter_worktree
- Generation contract: 2
- Variant: $variant
- Purpose: $purpose
- MCP: off
- Model integration: $model_integration

This records the original generation baseline, not the current application version
or which later starter changes have been applied. Only a clean checkout with an
exact local release tag records a release version. A modified checkout includes
local changes that the recorded commit cannot reproduce; retain that source copy.
Unknown values mean Git metadata was unavailable for this starter copy.

Runtime model names, endpoints and secrets are application settings, not generation options.
Older origin records without purpose mean API output with MCP off.

## Original generation options

Run from the matching starter checkout. Add --output-dir with a fresh directory
when comparing versions; keep the existing application separate.

EOF
  printf '\140\140\140bash\n./scripts/init-service.sh'
  printf ' \\\n  --%s %q' \
    variant "$variant" purpose "$purpose" service-name "$service_name" group-id "$group_id" \
    artifact-id "$artifact_id" package-name "$package_name" app-port "$app_port" management-port "$management_port"
  printf '\n\140\140\140\n\n'
  cat <<'EOF'
For manual updates, follow the [upgrade guide](https://github.com/dtkmn/spring-boot-playground/blob/main/docs/adoption/upgrading-generated-services.md).
Keep this origin record and append the target commit and selected changes after
each upgrade; replacing it with a new sample's record would lose the original baseline.
Preserve applied Flyway migration history; never replace a downstream schema with a fresh sample.
EOF
} > "$output_dir/STARTER.md"
trap - ERR
echo "Generated $variant ($purpose) starter at $output_dir"
