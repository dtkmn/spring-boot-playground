# Spring Service Starter

`spring-boot-playground` now acts as the repository for the Spring service starter program.

The default path is `mvc-jpa`. `webflux-r2dbc` remains the supported advanced variant for teams with a real reactive requirement. Optional integrations live under `examples/` and are intentionally outside the starter contract.

## Starter Contract

Every starter variant should provide:
- Java 21 and the maintained Spring Boot 3.5.x line
- PostgreSQL plus Flyway
- Actuator, Prometheus metrics, and OTLP-ready tracing hooks
- Docker Compose-backed local development plus standalone container smoke coverage
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

Generate a new service from the default variant:

```bash
./scripts/init-service.sh \
  --variant mvc-jpa \
  --service-name customer-profile \
  --group-id tech.company.platform \
  --artifact-id customer-profile \
  --package-name tech.company.platform.customerprofile
```

Output is created under `generated/<artifact-id>` by default.

Generated services now include:
- application code for the selected variant
- `.dockerignore` and `.gitignore`
- `gradlew`, `gradlew.bat`, and `gradle/wrapper`
- a starter CI workflow under `.github/workflows/ci.yml`
- a tag-gated publish workflow under `.github/workflows/publish.yml`
- Dependabot and baseline support/changelog docs
- `RELEASING.md` with branch and tag rules
- `compose.yaml` for `bootRun` development services
- `docker-compose.yml` for full container smoke validation
- `scripts/dev-smoke-test.sh` and `scripts/smoke-test.sh`
- local env template plus opt-in structured logging and OTLP tracing hooks
- a vendored Helm chart under `deploy/helm/spring-service-starter`
- service-specific Helm values for dev, staging, and prod

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
- Boot 4 migration is intentionally deferred to the next modernization tranche

## Governance

- `SUPPORT.md`: support window and update cadence
- `CHANGELOG.md`: release history
- `RELEASING.md`: release process and stabilization rules
- `CONTRIBUTING.md`: contribution and review rules
- `docs/adoption/pilot-playbook.md`: pilot execution and evidence rules
- `docs/releases/release-readiness-checklist.md`: `v1.0.0` readiness gates
- `docs/adr`: architectural decisions

## Root Repository Behavior

The repository root is no longer a runnable Spring application. Runtime code lives under `variants/` and `examples/`. This keeps the root of the repository focused on starter assets, governance, and validation.
