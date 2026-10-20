#!/usr/bin/env bash
set -Eeuo pipefail

fail() { echo "$*" >&2; exit 1; }
[[ $# == 1 && -n "$1" ]] || fail "Usage: ./scripts/package-plugin.sh <new-package-directory>"
script_dir=$(cd "$(dirname "$0")" && pwd -P)
repo_root=$(cd "$script_dir/.." && pwd -P)
destination=${1%/}
[[ ! -e "$destination" && ! -L "$destination" ]] || fail "Package directory already exists: $destination"
leaf=${destination##*/}
[[ -n "$leaf" && "$leaf" != . && "$leaf" != .. ]] || fail "Choose a new package directory"
parent=$(cd "$(dirname "$destination")" && pwd -P) || fail "Package parent must already exist"
destination="${parent%/}/$leaf"
case "$destination/" in "$repo_root/"*) fail "Package must be outside the source checkout" ;; esac

# shellcheck source=starter-provenance.sh
source "$script_dir/starter-provenance.sh"
read_starter_provenance "$repo_root" || fail "Could not read starter source provenance"
assets=(
  plugin.json .agents/plugins/marketplace.json skills/generate-service scripts/init-service.sh scripts/starter-provenance.sh
  variants/mvc-jpa/template variants/mvc-jpa/ai variants/webflux-r2dbc/template
  deploy/helm/spring-service-starter gradlew gradlew.bat gradle/wrapper LICENSE
)
for asset in "${assets[@]}"; do [[ -e "$repo_root/$asset" ]] || fail "Required package asset missing: $asset"; done
for variant in mvc-jpa/template mvc-jpa/ai webflux-r2dbc/template; do
  [[ -f "$repo_root/variants/$variant/.env.example" ]] || fail "Missing environment example: $variant"
done

mkdir "$destination"
trap 'rm -rf -- "$destination"' ERR
tar -C "$repo_root" \
  --exclude='.gradle' --exclude='build' --exclude='out' --exclude='.idea' \
  --exclude='.git' --exclude='.env' --exclude='.env.*' --exclude='*.log' \
  --exclude='*.iml' --exclude='.DS_Store' \
  -cf - "${assets[@]}" | tar -C "$destination" -xf -
for variant in mvc-jpa/template mvc-jpa/ai webflux-r2dbc/template; do
  cp "$repo_root/variants/$variant/.env.example" "$destination/variants/$variant/.env.example"
done
printf 'commit=%s\nversion=%s\nworktree=%s\n' \
  "$starter_commit" "$starter_version" "$starter_worktree" > "$destination/STARTER_SOURCE"
trap - ERR
echo "Packaged Java Seedworks at $destination"
