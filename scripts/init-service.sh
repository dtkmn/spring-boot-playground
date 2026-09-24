#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<USAGE
Usage:
  ./scripts/init-service.sh \
    --variant mvc-jpa|webflux-r2dbc \
    --service-name <name> \
    --group-id <group> \
    --artifact-id <artifact> \
    --package-name <package> \
    [--output-dir <dir>] \
    [--app-port <port>] \
    [--management-port <port>]
USAGE
}

variant=""
service_name=""
group_id=""
artifact_id=""
package_name=""
output_dir=""
app_port="8080"
management_port="8081"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --variant)
      variant="$2"
      shift 2
      ;;
    --service-name)
      service_name="$2"
      shift 2
      ;;
    --group-id)
      group_id="$2"
      shift 2
      ;;
    --artifact-id)
      artifact_id="$2"
      shift 2
      ;;
    --package-name)
      package_name="$2"
      shift 2
      ;;
    --output-dir)
      output_dir="$2"
      shift 2
      ;;
    --app-port)
      app_port="$2"
      shift 2
      ;;
    --management-port)
      management_port="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$variant" || -z "$service_name" || -z "$group_id" || -z "$artifact_id" || -z "$package_name" ]]; then
  usage
  exit 1
fi

case "$variant" in
  mvc-jpa|webflux-r2dbc)
    ;;
  *)
    echo "Unsupported variant: $variant" >&2
    exit 1
    ;;
esac

script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$script_dir/.." && pwd)
template_dir="$repo_root/variants/$variant/template"
shared_chart_dir="$repo_root/deploy/helm/spring-service-starter"
output_dir=${output_dir:-"$repo_root/generated/$artifact_id"}
package_path=${package_name//./\/}

if [[ ! -d "$template_dir" ]]; then
  echo "Template directory not found: $template_dir" >&2
  exit 1
fi

if [[ -e "$output_dir" ]]; then
  echo "Output directory already exists: $output_dir" >&2
  exit 1
fi

mkdir -p "$output_dir"
# Raw templates may have been opened in an IDE or built locally. Do not carry
# their caches, compiled classes, credentials, or workspace metadata forward.
tar -C "$template_dir" \
  --exclude='.gradle' --exclude='build' --exclude='out' \
  --exclude='.idea' --exclude='*.iml' --exclude='.git' \
  --exclude='.env' --exclude='.DS_Store' --exclude='*.log' \
  -cf - . | tar -C "$output_dir" -xf -
cp "$repo_root/gradlew" "$output_dir/"
cp "$repo_root/gradlew.bat" "$output_dir/"
cp "$repo_root/LICENSE" "$output_dir/"
mkdir -p "$output_dir/gradle"
cp -R "$repo_root/gradle/wrapper" "$output_dir/gradle/"

if [[ -d "$shared_chart_dir" ]]; then
  mkdir -p "$output_dir/deploy/helm"
  cp -R "$shared_chart_dir" "$output_dir/deploy/helm/"
fi

while IFS= read -r -d '' dir; do
  target_dir=$(dirname "$dir")/$package_path
  mkdir -p "$(dirname "$target_dir")"
  mv "$dir" "$target_dir"
done < <(find "$output_dir" -depth -type d -name '__PACKAGE_PATH__' -print0)

while IFS= read -r -d '' file; do
  # An empty delimiter reads to EOF, preserving trailing newlines. Success
  # means a NUL byte was found: leave that binary asset untouched.
  if IFS= read -r -d '' content < "$file"; then
    continue
  fi
  original_content=$content
  # Quoted replacements keep characters such as & and backslashes literal.
  content=${content//__SERVICE_NAME__/"$service_name"}
  content=${content//__GROUP_ID__/"$group_id"}
  content=${content//__ARTIFACT_ID__/"$artifact_id"}
  content=${content//__PACKAGE_NAME__/"$package_name"}
  content=${content//__APP_PORT__/"$app_port"}
  content=${content//__MANAGEMENT_PORT__/"$management_port"}
  if [[ "$content" != "$original_content" ]]; then
    printf '%s' "$content" > "$file"
  fi
done < <(find "$output_dir" -type f -print0)

echo "Generated $variant starter at $output_dir"
