# __SERVICE_NAME__

Spring WebFlux + R2DBC starter service generated from the Spring Service Starter repository.

## Stack
- Java 21
- Spring Boot
- Spring WebFlux
- Spring Data R2DBC
- PostgreSQL
- Flyway
- RFC 9457 problem details
- Actuator + Prometheus + OTLP-ready tracing
- Docker + Docker Compose
- CycloneDX SBOM output
- JaCoCo coverage reports
- Helm for Kubernetes deployment

## Local development

```bash
cp .env.example .env
./gradlew test
./gradlew check
./gradlew bootRun
./scripts/dev-smoke-test.sh
./scripts/smoke-test.sh
```

`./gradlew bootRun` uses `compose.yaml` plus Spring Boot development services to start PostgreSQL automatically when Docker is available.

## Local container workflow

```bash
cp .env.example .env
docker compose -f docker-compose.yml --env-file .env up --build
```

Default ports:
- app host: `__APP_PORT__`
- management host: `__MANAGEMENT_PORT__`
- PostgreSQL export: `5432`

Override `APP_HOST_PORT`, `MANAGEMENT_HOST_PORT`, or `POSTGRES_EXPORT_PORT` in `.env` if those ports are already in use locally.

## HTTP API
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`

The starter seeds one customer record through Flyway so health and API checks have immediate data.

## Error contract

Application errors use RFC `9457` problem details with:
- `type`
- `title`
- `status`
- `detail`
- `instance`
- `errors` for validation failures only

## Observability

Prometheus metrics are exposed at `/actuator/prometheus` on the management port. Spring Boot's OpenTelemetry starter enables request tracing and adds `traceId` and `spanId` to logs written within a trace. The default sampling probability is 10%; tune it with `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` (from `0.0` to `1.0`).

Automatic Reactor context propagation is enabled so trace context and log correlation survive asynchronous boundaries in reactive request handling.

OTLP metric export is disabled by default; Prometheus remains the metrics backend. No collector is required for local development: traces are not exported until an OTLP endpoint is configured. With an OTLP HTTP collector running locally, enable export and sample every request for troubleshooting:

```bash
MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT=http://localhost:4318/v1/traces \
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0 \
./gradlew bootRun
```

Spring Boot 4 uses `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT`; replace the old `MANAGEMENT_OTLP_TRACING_ENDPOINT` setting. Pass the new variable to the application process or container at runtime. For Helm, use the chart's `env` map with a collector address reachable from the pod:

```yaml
env:
  MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT: http://otel-collector:4318/v1/traces
```

Structured JSON console logs are opt-in with `SPRING_PROFILES_ACTIVE=structured-logging`; they include trace correlation fields when a trace is active. See [Spring Boot tracing](https://docs.spring.io/spring-boot/4.1/reference/actuator/tracing.html) for configuration and propagation details.

## Supply chain

`./gradlew check` generates a CycloneDX SBOM under `build/reports/cyclonedx/` and JaCoCo coverage output under `build/reports/jacoco/`. CI uploads these reports as build artifacts.

The production image uses a digest-pinned distroless Java runtime and runs as a non-root user. The packaged-container smoke test verifies that the application starts and serves its health and API endpoints. Dependabot tracks Gradle, GitHub Actions, and Docker updates.

The production image intentionally has no shell or package manager. Use application logs, Actuator, metrics, traces, JVM diagnostics, or an ephemeral debug container for investigation. A service that requires OS packages, native libraries, fonts, or shell execution must document and test its runtime-image exception.

## Kubernetes deployment

Create the database secret:

```bash
kubectl create secret generic __ARTIFACT_ID__-db \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=change-me
```

Render or deploy with Helm:

```bash
helm lint deploy/helm/spring-service-starter
helm template __ARTIFACT_ID__ deploy/helm/spring-service-starter -f deploy/helm/values-dev.yaml
helm upgrade --install __ARTIFACT_ID__ deploy/helm/spring-service-starter \
  -f deploy/helm/values-dev.yaml \
  --set image.repository=ghcr.io/your-org/__ARTIFACT_ID__ \
  --set image.tag=latest
```

Service-specific deployment values live under `deploy/helm/`.
Optional hardening values include service-account controls, pod annotations, pod/container security contexts, node scheduling hints, and a pod disruption budget.

## Release workflow

Create and push a `v*` tag to publish the container image to GHCR:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Release tags must be created from commits already promoted to `main`. See `RELEASING.md` for the full release checklist.
