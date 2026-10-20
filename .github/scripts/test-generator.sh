#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
trial_dir=$(mktemp -d "${TMPDIR:-/tmp}/starter-generator-contract.XXXXXX")
trap 'rm -rf -- "$trial_dir"' EXIT
source_dir="$trial_dir/source"
mkdir -p "$source_dir"

# Mutate only a disposable source snapshot; never the user's templates or working tree.
tar -C "$repo_root" \
  --exclude='.gradle' --exclude='build' --exclude='out' --exclude='.idea' \
  --exclude='.git' --exclude='.env' --exclude='*.log' --exclude='*.iml' --exclude='.DS_Store' \
  -cf - scripts/init-service.sh scripts/starter-provenance.sh scripts/package-plugin.sh \
  plugin.json .agents/plugins/marketplace.json skills/generate-service variants/mvc-jpa/template variants/webflux-r2dbc/template \
  variants/mvc-jpa/ai gradlew gradlew.bat gradle/wrapper LICENSE deploy/helm/spring-service-starter \
  | tar -C "$source_dir" -xf -

generator="$source_dir/scripts/init-service.sh"
coordinates=(--service-name contract-service --group-id tech.example \
  --artifact-id contract-service --package-name tech.example.contract)
common=(--variant mvc-jpa "${coordinates[@]}")

# A binary contains a replacement token deliberately; preserving its bytes is the contract.
template="$source_dir/variants/mvc-jpa/template"
printf '\000__SERVICE_NAME__\377\n' > "$template/contract-binary.bin"
mkdir -p "$template/.gradle" "$template/build" "$template/out" "$template/.idea"
for path in .env .gradle/cache build/classes out/classes .idea/workspace.xml private.log; do
  printf 'disposable exclusion fixture\n' > "$template/$path"
done

"$generator" "${common[@]}" --output-dir "$trial_dir/default" >/dev/null
"$generator" "${common[@]}" --purpose api --output-dir "$trial_dir/api" >/dev/null
"$generator" --variant webflux-r2dbc "${coordinates[@]}" --purpose api --output-dir "$trial_dir/webflux" >/dev/null
diff -r -x STARTER.md "$trial_dir/default" "$trial_dir/api" >/dev/null
cmp "$template/contract-binary.bin" "$trial_dir/default/contract-binary.bin"
if "$generator" "${common[@]}" --purpose ai --output-dir "$trial_dir/default" >"$trial_dir/rejection.log" 2>&1; then
  echo "Generator overwrote an existing application directory" >&2
  exit 1
fi
[[ ! -e "$trial_dir/default/gradle/ai.gradle" ]]
cmp "$template/contract-binary.bin" "$trial_dir/default/contract-binary.bin"
for path in .env .gradle build out .idea private.log; do
  [[ ! -e "$trial_dir/default/$path" ]] || { echo "Excluded asset copied: $path" >&2; exit 1; }
done

for output in default api webflux; do
  grep -Fxq -- '- Generation contract: 2' "$trial_dir/$output/STARTER.md"
  grep -Fxq -- '- Purpose: api' "$trial_dir/$output/STARTER.md"
  grep -Fxq -- '- MCP: off' "$trial_dir/$output/STARTER.md"
  grep -Fxq -- '- Model integration: none' "$trial_dir/$output/STARTER.md"
  [[ ! -e "$trial_dir/$output/gradle/ai.gradle" ]]
  [[ ! -e "$trial_dir/$output/src/main/resources/application-ai.yaml" ]]
  if grep -REq 'org\.springframework\.ai|PILOT_CALLER_[AB]_TOKEN|OLLAMA_' \
      "$trial_dir/$output/build.gradle" "$trial_dir/$output/src" "$trial_dir/$output/.env.example"; then
    echo "Ordinary API output includes AI dependencies, configuration or credentials" >&2
    exit 1
  fi
done

"$generator" "${common[@]}" --purpose ai --output-dir "$trial_dir/ai" >/dev/null
"$generator" "${common[@]}" --purpose ai --output-dir "$trial_dir/ai-repeat" >/dev/null
diff -r "$trial_dir/ai" "$trial_dir/ai-repeat" >/dev/null
grep -Fxq -- '- Purpose: ai' "$trial_dir/ai/STARTER.md"
grep -Fxq -- '- Model integration: ollama' "$trial_dir/ai/STARTER.md"
[[ -f "$trial_dir/ai/gradle/ai.gradle" ]]
[[ -f "$trial_dir/ai/src/main/resources/application-ai.yaml" ]]
[[ ! -e "$trial_dir/ai/deploy/helm" && ! -e "$trial_dir/ai/.env" ]]
[[ -z "$(find "$trial_dir/ai/src" -type f -name '*Customer*' -print -quit)" ]]
if grep -RIqE '__[A-Z_]+__' "$trial_dir/ai"; then
  echo "AI output contains unresolved text placeholders" >&2
  exit 1
fi

expect_rejected() {
  local case_name=$1
  shift
  local destination="$trial_dir/rejected-$case_name"
  if "$generator" "${common[@]}" --output-dir "$destination" "$@" >"$trial_dir/rejection.log" 2>&1; then
    echo "Invalid generation unexpectedly succeeded: $case_name" >&2
    exit 1
  fi
  [[ ! -e "$destination" ]] || { echo "Rejected input created output: $case_name" >&2; exit 1; }
}

expect_rejected unknown --not-a-choice value
expect_rejected missing-value --purpose
expect_rejected unsupported-purpose --purpose agent
expect_rejected unsupported-mcp --mcp on
expect_rejected unsupported-variant --variant quarkus
expect_rejected unsupported-ai-variant --variant webflux-r2dbc --purpose ai
expect_rejected package --package-name tech.example.invalid-package
expect_rejected keyword --package-name tech.example.class
expect_rejected artifact --artifact-id '../outside'
expect_rejected group --group-id 'invalid group'
expect_rejected service --service-name 'invalid/service'
expect_rejected port-zero --app-port 0
expect_rejected port-range --management-port 65536
expect_rejected port-text --app-port invalid
expect_rejected equal-ports --management-port 8080
expect_rejected equal-normalized-ports --management-port 08080

# Existing files/links and source assets must survive rejected output paths.
printf 'keep this parent\n' > "$trial_dir/file-parent"
expect_rejected file-parent --output-dir "$trial_dir/file-parent/child"
grep -Fxq 'keep this parent' "$trial_dir/file-parent"
ln -s "$trial_dir/absent-target" "$trial_dir/dangling"
expect_rejected dangling-link --output-dir "$trial_dir/dangling"
[[ -L "$trial_dir/dangling" && ! -e "$trial_dir/absent-target" ]]
expect_rejected source-output --output-dir "$template/forbidden-output"
[[ ! -e "$template/forbidden-output" ]]
ln -s "$template" "$trial_dir/source-link"
expect_rejected source-parent-link --output-dir "$trial_dir/source-link/forbidden-output"
[[ ! -e "$template/forbidden-output" ]]

mv "$source_dir/deploy/helm/spring-service-starter/templates/_helpers.tpl" "$trial_dir/helpers.saved"
expect_rejected missing-chart-helper
mv "$trial_dir/helpers.saved" "$source_dir/deploy/helm/spring-service-starter/templates/_helpers.tpl"

mv "$source_dir/LICENSE" "$source_dir/LICENSE.saved"
expect_rejected missing-license
mv "$source_dir/LICENSE.saved" "$source_dir/LICENSE"
mv "$source_dir/variants/mvc-jpa/ai/gradle/ai.gradle" "$source_dir/variants/mvc-jpa/ai/gradle/ai.gradle.saved"
expect_rejected missing-ai-asset --purpose ai
mv "$source_dir/variants/mvc-jpa/ai/gradle/ai.gradle.saved" "$source_dir/variants/mvc-jpa/ai/gradle/ai.gradle"

# A failure after mkdir must remove only this invocation's new output.
mkdir "$trial_dir/fault-bin"
cat > "$trial_dir/fault-bin/tar" <<'TAR_FAILURE'
#!/usr/bin/env bash
exit 42
TAR_FAILURE
chmod +x "$trial_dir/fault-bin/tar"
PATH="$trial_dir/fault-bin:$PATH" expect_rejected extraction-failure
[[ -f "$trial_dir/default/README.md" ]]

# Package a clean tagged fixture, then run without its Git metadata from another
# working directory. The package and manual CLI must compose the same service.
git -C "$source_dir" init -q
git -C "$source_dir" add .
git -C "$source_dir" -c user.name='Generator contract' -c user.email='contract@example.invalid' \
  -c commit.gpgsign=false commit -qm 'Disposable package fixture'
git -C "$source_dir" tag v9.8.7
fixture_commit=$(git -C "$source_dir" rev-parse HEAD)
package_dir="$trial_dir/package with spaces"
bash "$source_dir/scripts/package-plugin.sh" "$package_dir" >/dev/null
cmp "$source_dir/.agents/plugins/marketplace.json" "$package_dir/.agents/plugins/marketplace.json"
[[ ! -e "$package_dir/.git" && ! -e "$package_dir/variants/mvc-jpa/template/.env" ]]
grep -Fxq "commit=$fixture_commit" "$package_dir/STARTER_SOURCE"
grep -Fxq 'version=v9.8.7' "$package_dir/STARTER_SOURCE"
grep -Fxq 'worktree=clean' "$package_dir/STARTER_SOURCE"
mkdir "$trial_dir/unrelated"
for selection in mvc-jpa:api webflux-r2dbc:api mvc-jpa:ai; do
  variant=${selection%:*}
  purpose=${selection#*:}
  manual="$trial_dir/manual-$variant-$purpose"
  packaged="$trial_dir/packaged-$variant-$purpose"
  "$generator" --variant "$variant" --purpose "$purpose" "${coordinates[@]}" --output-dir "$manual" >/dev/null
  (cd "$trial_dir/unrelated" && bash "$package_dir/scripts/init-service.sh" \
    --plugin --variant "$variant" --purpose "$purpose" "${coordinates[@]}" --output-dir "$packaged") >/dev/null
  diff -r "$manual" "$packaged" >/dev/null
  grep -Fxq -- "- Source commit: $fixture_commit" "$packaged/STARTER.md"
done

# Git-backed installations retain Git metadata instead of a packaged stamp.
# Plugin mode must still protect that installation, without changing the normal
# checkout workflow. All three plugin outputs must match the canonical CLI.
for selection in mvc-jpa:api webflux-r2dbc:api mvc-jpa:ai; do
  variant=${selection%:*}
  purpose=${selection#*:}
  git_plugin="$trial_dir/git-plugin-$variant-$purpose"
  bash "$generator" --plugin --variant "$variant" --purpose "$purpose" "${coordinates[@]}" \
    --output-dir "$git_plugin" >/dev/null
  diff -r "$trial_dir/manual-$variant-$purpose" "$git_plugin" >/dev/null
done
[[ -z "$(git -C "$source_dir" status --porcelain)" ]]
if bash "$generator" --plugin "${common[@]}" >"$trial_dir/rejection.log" 2>&1; then
  echo "Plugin mode accepted an implicit destination" >&2; exit 1
fi
ln -s "$source_dir" "$trial_dir/installation-link"
for destination in "$source_dir/generated/unsafe" "$trial_dir/installation-link/unsafe"; do
  if bash "$generator" --plugin "${common[@]}" --output-dir "$destination" \
      >"$trial_dir/rejection.log" 2>&1; then
    echo "Plugin mode accepted output inside a Git installation" >&2; exit 1
  fi
done
[[ ! -e "$source_dir/generated" && ! -e "$source_dir/unsafe" ]]

if bash "$source_dir/scripts/package-plugin.sh" "$package_dir" >"$trial_dir/rejection.log" 2>&1; then
  echo "Packager overwrote an existing package" >&2; exit 1
fi
for destination in "$package_dir/unwanted-output" "$trial_dir/unrelated/existing"; do
  [[ "$destination" != */existing ]] || mkdir "$destination"
  if bash "$package_dir/scripts/init-service.sh" "${common[@]}" --output-dir "$destination" \
      >"$trial_dir/rejection.log" 2>&1; then
    echo "Packaged generator accepted an unsafe destination" >&2; exit 1
  fi
done
[[ ! -e "$package_dir/unwanted-output" ]]

# A containing checkout must not replace the stamped upstream identity.
git -C "$trial_dir" init -q
bash "$package_dir/scripts/init-service.sh" "${common[@]}" --output-dir "$trial_dir/inside-other-repo" >/dev/null
grep -Fxq -- "- Source commit: $fixture_commit" "$trial_dir/inside-other-repo/STARTER.md"
cp "$package_dir/STARTER_SOURCE" "$trial_dir/origin.saved"
printf 'version=invalid\n' >> "$package_dir/STARTER_SOURCE"
if bash "$package_dir/scripts/init-service.sh" "${common[@]}" --output-dir "$trial_dir/invalid-origin" \
    >"$trial_dir/rejection.log" 2>&1; then
  echo "Malformed package provenance was accepted" >&2; exit 1
fi
[[ ! -e "$trial_dir/invalid-origin" ]]
cp "$trial_dir/origin.saved" "$package_dir/STARTER_SOURCE"

mkdir "$trial_dir/git-fault-bin"
cat > "$trial_dir/git-fault-bin/git" <<'GIT_FAILURE'
#!/usr/bin/env bash
[[ "$3" != status ]] || exit 77
exec "$STARTER_CONTRACT_GIT" "$@"
GIT_FAILURE
chmod +x "$trial_dir/git-fault-bin/git"
if STARTER_CONTRACT_GIT="$(command -v git)" PATH="$trial_dir/git-fault-bin:$PATH" \
    bash "$generator" "${common[@]}" --output-dir "$trial_dir/status-failure" \
    >"$trial_dir/rejection.log" 2>&1; then
  echo "Git status failure was reported as a clean source" >&2; exit 1
fi
[[ ! -e "$trial_dir/status-failure" ]]

printf '\nDisposable local change\n' >> "$source_dir/variants/mvc-jpa/template/README.md"
bash "$source_dir/scripts/package-plugin.sh" "$trial_dir/modified-package" >/dev/null
grep -Fxq 'worktree=modified' "$trial_dir/modified-package/STARTER_SOURCE"
grep -Fxq 'version=unreleased' "$trial_dir/modified-package/STARTER_SOURCE"

if PATH="$trial_dir/fault-bin:$PATH" bash "$source_dir/scripts/package-plugin.sh" "$trial_dir/failed-package" \
    >"$trial_dir/rejection.log" 2>&1; then
  echo "Packager ignored an extraction failure" >&2; exit 1
fi
[[ ! -e "$trial_dir/failed-package" && -f "$package_dir/STARTER_SOURCE" ]]

# The traditional checkout still supports its default generated/ destination.
bash "$generator" "${common[@]}" >/dev/null
[[ -f "$source_dir/generated/contract-service/STARTER.md" ]]

echo "Generator and plugin packaging: selection, parity, provenance, exclusions and rejection contracts passed."
