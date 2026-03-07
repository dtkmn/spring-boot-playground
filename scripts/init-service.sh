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
cp -R "$template_dir/." "$output_dir"

while IFS= read -r -d '' dir; do
  target_dir=$(dirname "$dir")/$package_path
  mkdir -p "$(dirname "$target_dir")"
  mv "$dir" "$target_dir"
done < <(find "$output_dir" -depth -type d -name '__PACKAGE_PATH__' -print0)

replace_tokens() {
  local file="$1"
  perl -0pi -e 's/__SERVICE_NAME__/$ENV{SERVICE_NAME}/g; s/__GROUP_ID__/$ENV{GROUP_ID}/g; s/__ARTIFACT_ID__/$ENV{ARTIFACT_ID}/g; s/__PACKAGE_NAME__/$ENV{PACKAGE_NAME}/g; s/__APP_PORT__/$ENV{APP_PORT}/g; s/__MANAGEMENT_PORT__/$ENV{MANAGEMENT_PORT}/g;' "$file"
}

export SERVICE_NAME="$service_name"
export GROUP_ID="$group_id"
export ARTIFACT_ID="$artifact_id"
export PACKAGE_NAME="$package_name"
export APP_PORT="$app_port"
export MANAGEMENT_PORT="$management_port"

while IFS= read -r -d '' file; do
  replace_tokens "$file"
done < <(find "$output_dir" -type f -print0)

echo "Generated $variant starter at $output_dir"
