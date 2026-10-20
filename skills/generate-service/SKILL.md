---
name: generate-service
description: Generate and verify a new Java Seedworks service from the bundled Spring MVC/JPA or WebFlux/R2DBC starter, with an optional experimental MVC AI purpose.
---

# Generate a service

Use the bundled generator to create an independent service. The manual CLI and
this skill use the same templates and generation contract.

## Choose the output

Default to `mvc-jpa` and purpose `api`. Use `webflux-r2dbc` for an explicit
reactive requirement.

| Variant | Purpose | Support |
| --- | --- | --- |
| `mvc-jpa` | `api` | Maintained; host, Compose, Helm |
| `webflux-r2dbc` | `api` | Maintained; host, Compose, Helm |
| `mvc-jpa` | `ai` | Experimental order-support example; host/local Compose |

API output starts with the Customer sample domain. A project name does not
replace that sample with a custom business API; explain the starting domain and
where it needs adapting when handing off the generated service.

The AI output uses opt-in Ollama classification followed by Java-owned lookups.
Its default Order API requires fresh local bearer credentials and runs without
a model. Regular tests use a fake model endpoint. It has no Helm chart or
production identity integration. Using this skill for an API adds no runtime AI.

Collect the service name, group ID, artifact ID, Java package and destination.
Infer values from the request or project conventions; suggest matching service
and artifact names. Ask for critical missing choices and resolve unsupported
requirements before writing output. Ports default to `8080` and `8081`.

## Run the bundled generator

Locate this skill's absolute `SKILL.md` path from the loaded skill. Set
`skill_file` to that path and derive the plugin root from it, independently of
the shell working directory:

```bash
plugin_root="$(cd "$(dirname "$skill_file")/../.." && pwd -P)"
```

Use a new absolute destination in the user's workspace, outside the plugin
installation/cache. The generator rejects existing directories and symlinks.
Generation needs Bash, `tar`, `find` and standard Unix tools on macOS, Linux or
WSL2. Java and Docker are needed for verification, not for copying the scaffold.

Set the selected values and pass them as quoted arguments:

```bash
generator_args=(
  --plugin
  --variant "$variant" --purpose "$purpose"
  --service-name "$service_name" --group-id "$group_id"
  --artifact-id "$artifact_id" --package-name "$package_name"
  --output-dir "$output_dir"
)
# Append --app-port / --management-port when the user selected other ports.
bash "$plugin_root/scripts/init-service.sh" "${generator_args[@]}"
```

Always pass `--plugin`: it requires an explicit destination outside the
installation, including Git-backed caches that retain their own Git metadata.

If a packaged asset is missing, report an incomplete installation instead of
reconstructing the starter with model-written replacement files.

## Verify and hand off

Read the generated `README.md` and `STARTER.md`. With a Java 21 JDK and a running
Docker engine, run `./gradlew check bootJar` once from the generated directory.
Integration tests use disposable PostgreSQL containers. Follow the README for
first-run configuration and Compose v2 commands. If prerequisites are unavailable,
keep the generated project and report the checks that could not run.

Model inference remains opt-in. Tool installation, model downloads, deployment
and Git publication require their own user request; they are not generation steps.

Report the absolute output path, selected variant/purpose and coordinates,
verification results and relevant first-run commands. Describe source version,
commit and working-tree state as recorded in `STARTER.md`; a development plugin
version does not establish a released starter baseline.
Git-backed installs can retain Git metadata; if neither that metadata nor a
packaged source stamp is available, report `unknown` rather than guessing.
