# Spring Service Starter

`spring-boot-playground` is a practical, public starter for Spring services, with
MVC/JPA as the default and WebFlux/R2DBC for reactive workloads.

## Release Scope

This repository provides a reusable Spring service starter. The v1.0 scope covers service generation, local development, container packaging, and Kubernetes deployment.

Both variants have been validated as generated services with PostgreSQL, Docker Compose, and local Kubernetes. See [adoption guidance](docs/adoption/promotion-brief.md) and the [version policy](docs/releases/version-policy.md). The current baseline is Java 21 plus Spring Boot 4.1.1.

The default path is `mvc-jpa`. `webflux-r2dbc` remains the supported advanced variant for teams with a real reactive requirement. Optional integrations live under `examples/` and are intentionally outside the starter contract.

## Starter Contract

Every starter variant should provide:
- Java 21 and the maintained Spring Boot 4.1.x line
- PostgreSQL plus Flyway
- Actuator, Prometheus metrics, and OTLP-ready tracing hooks
- Docker Compose-backed local development plus standalone container smoke coverage
- digest-pinned, non-root distroless Java runtime
- test baselines suitable for CI
- Kubernetes deployment support through Helm

The default starter contract does not include:
- Kafka and Kafka Streams
- websocket ingest pipelines
- domain-specific integrations
- a broad demo runtime at the repository root

## Repository Layout

- `variants/mvc-jpa`: default starter and recommended path for most services
- `variants/webflux-r2dbc`: supported advanced variant for reactive workloads
- `examples/kafka-basic`: minimal HTTP-to-Kafka example
- `examples/kafka-streams`: isolated stream-processing example
- `examples/binance-websocket`: websocket-to-Kafka market-data example
- `deploy/helm/spring-service-starter`: shared Kubernetes deployment chart
- `docs/adr`: architecture decision records
- `docs/adoption`: pilot execution guidance
- `docs/releases`: release readiness checklists
- `scripts/init-service.sh`: starter bootstrap script

## Start A New Service

Prerequisites: Git, a Java 21 JDK, a running Docker engine with Docker Compose v2,
and Bash with standard Unix tools (`tar`, `find`, and `curl`). Use macOS, Linux,
or WSL2.
The generated Gradle wrapper downloads Gradle; no separate Gradle installation is
needed. The first run also downloads dependencies and container images.

Start from the stabilized `main` branch in a new directory:

```bash
git clone --branch main --depth 1 https://github.com/dtkmn/spring-boot-playground.git
cd spring-boot-playground
./scripts/init-service.sh \
  --variant mvc-jpa \
  --service-name customer-profile \
  --group-id tech.company.platform \
  --artifact-id customer-profile \
  --package-name tech.company.platform.customerprofile
cd generated/customer-profile
cp .env.example .env
./gradlew bootRun
```

Keep that terminal running. Spring Boot starts PostgreSQL through `compose.yaml`
and Flyway creates the schema and seed data. Once the application has started,
run this request in a second terminal:

```bash
curl -fsS http://localhost:8080/api/v1/customers
```

A fresh database returns:

```json
[{"id":1,"firstName":"John","lastName":"Doe"}]
```

Press Ctrl-C in the first terminal to stop the application and its managed database.
Restart with `./gradlew bootRun` from `generated/customer-profile`; keep the database
container to retain local data. For build/test commands, port overrides, and where
to extend the application, see the current
[MVC/JPA instructions](variants/mvc-jpa/template/README.md) or
[WebFlux/R2DBC instructions](variants/webflux-r2dbc/template/README.md).

For the reactive variant, choose `--variant webflux-r2dbc` and an unused artifact
name/output directory. The generation and first-run steps are otherwise the same.
The `dev-smoke-test.sh` and `smoke-test.sh` scripts are optional, disposable checks:
they remove containers and database volumes before and after running. Use a
separate generated copy for smoke checks when you want to keep development data.

Output is created under `generated/<artifact-id>` by default.

The raw templates use the repository's shared Gradle wrapper. The generator
copies that wrapper into each generated service; do not add separate wrappers
under `variants/*/template`. In IntelliJ IDEA, import a generated service as a
Gradle project. Linking a raw template can cause the IDE to generate another
wrapper inside it.

To pin a release, replace `--branch main` with a tag such as `--branch v1.0.0`.
That older release still requires Perl for generation and predates bundled license
files; copy [LICENSE](LICENSE) into services generated from it to retain the
starter's copyright and permission notice. Generation from this checkout includes
the assets below.

Generated services include:
- application code for the selected variant
- the starter's MIT license and copyright notice
- `STARTER.md` with the source commit, release status, variant, and original generation options
- `.dockerignore` and `.gitignore`
- `gradlew`, `gradlew.bat`, and `gradle/wrapper`
- a starter CI workflow under `.github/workflows/ci.yml`
- a tag-gated publish workflow under `.github/workflows/publish.yml`
- Dependabot and baseline support/changelog docs
- CycloneDX SBOM generation through `./gradlew check`
- JaCoCo coverage reports from the default test lifecycle
- `RELEASING.md` with branch and tag rules
- `compose.yaml` for `bootRun` development services
- `docker-compose.yml` for full container smoke validation
- `scripts/dev-smoke-test.sh` and `scripts/smoke-test.sh`
- local env template plus opt-in structured logging and OTLP tracing hooks
- digest-pinned distroless Java runtime running as non-root
- a vendored Helm chart under `deploy/helm/spring-service-starter`
- service-specific Helm values for dev, staging, and prod

For an existing service, follow the [manual upgrade guide](docs/adoption/upgrading-generated-services.md)
to compare starter versions and apply relevant changes while preserving application code.

## Variants

### `mvc-jpa`
Use this when:
- the service is request/response heavy
- the team wants the lowest-friction Spring path
- JPA and standard blocking I/O are acceptable

### `webflux-r2dbc`
Use this when:
- the service has a real reactive requirement
- the team is comfortable with Reactor and reactive persistence
- non-blocking I/O provides clear value

## Examples

Examples are runnable, isolated projects. They exist to show how to add optional integrations without forcing those choices into every new service.

Run an example directly from its directory, for example:

```bash
./gradlew -p examples/kafka-basic test
cp examples/kafka-basic/.env.example examples/kafka-basic/.env
cd examples/kafka-basic && docker compose --env-file .env up --build
```

## Kubernetes And Helm

The supported deployment path lives in `deploy/helm/spring-service-starter`.

Supported chart contract:
- `image.repository`
- `image.tag`
- `service.port`
- `env`
- `secrets.existingSecret`
- `serviceAccount`
- `podAnnotations`
- `podSecurityContext`
- `containerSecurityContext`
- `resources`
- `autoscaling.enabled`
- `ingress.enabled`
- `ingress.hosts`
- `nodeSelector`
- `tolerations`
- `affinity`
- `podDisruptionBudget`
- `postgres.enabled`
- `postgres.host`
- `postgres.port`
- `postgres.database`
- `management.port`

Default deployment assumptions:
- external PostgreSQL
- no bundled Kafka
- Actuator-backed health probes
- HPA support is optional and disabled by default
- Java 25 migration remains a separate modernization tranche

## Governance

- `SUPPORT.md`: support window and update cadence
- `CHANGELOG.md`: release history
- `RELEASING.md`: release process and stabilization rules
- `CONTRIBUTING.md`: contribution and review rules
- `docs/adoption/pilot-playbook.md`: pilot execution and evidence rules
- `docs/adoption/promotion-brief.md`: adoption guidance and validation boundaries
- `docs/releases/release-readiness-checklist.md`: `v1.0.0` readiness gates
- `docs/releases/version-policy.md`: Java, Spring Boot, Gradle, and migration policy
- `docs/security/supply-chain-baseline.md`: SBOM, Dependabot, and scanner guardrails
- `docs/adr`: architectural decisions

## Root Repository Behavior

The repository root is no longer a runnable Spring application. Runtime code lives under `variants/` and `examples/`. This keeps the root of the repository focused on starter assets, governance, and validation.

## License

The starter's original code, documentation, and templates are available under the
[MIT License](LICENSE). The generator in this checkout includes a copy of this
license for the copied scaffold. You may use and modify it in public, private, or
commercial applications; retain the copyright and permission notice when distributing copies
or substantial portions. Third-party components, including the Gradle wrapper,
retain their own licenses and notices.
