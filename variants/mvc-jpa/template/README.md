# __SERVICE_NAME__

Spring MVC + JPA starter service generated from the Spring Service Starter repository.

The copied starter scaffold is covered by the [MIT License](LICENSE). Retain its
copyright and permission notice when distributing copies or substantial portions.
Third-party components, including the Gradle wrapper, retain their own licenses
and notices. The application owner chooses the license for their own additions.

## Stack
- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- RFC 9457 problem details
- Actuator + Prometheus + OTLP-ready tracing
- Docker + Docker Compose
- CycloneDX SBOM output
- JaCoCo coverage reports
- Helm for Kubernetes deployment

## Local development

Run commands from this generated service directory with a Java 21 JDK and a
running Docker engine with Docker Compose v2. The Gradle wrapper installs Gradle
on its first run. Bash and `curl` are also required for the smoke scripts.

On first setup, copy the local database configuration:

```bash
cp .env.example .env
```

Keep `.env` local; it is ignored by Git. Start the application in the foreground:

```bash
./gradlew bootRun
```

Spring Boot uses `compose.yaml` to start PostgreSQL on an available host port and
configure the database connection. Flyway creates the schema and seeds a customer.
Once startup completes, use a second terminal:

```bash
curl -fsS http://localhost:__APP_PORT__/api/v1/customers
```

A fresh database returns:

```json
[{"id":1,"firstName":"John","lastName":"Doe"}]
```

Health is available at `http://localhost:__MANAGEMENT_PORT__/actuator/health`.
Press Ctrl-C in the application terminal to stop the application and its managed
PostgreSQL container. Restart with `./gradlew bootRun`; data survives while you keep
the database container. If you started PostgreSQL separately, stop it with
`docker compose -f compose.yaml --env-file .env stop`.

If the application ports are occupied, set process environment variables:

```bash
SERVER_PORT=18080 MANAGEMENT_SERVER_PORT=18081 ./gradlew bootRun
```

Use the overridden ports in requests. `APP_HOST_PORT` and `MANAGEMENT_HOST_PORT`
in `.env` configure the container workflow and smoke scripts, not a plain `bootRun`.

## Build, test, and extend

- `./gradlew check`: run the tests and produce coverage and SBOM reports. Integration tests use disposable PostgreSQL containers, so Docker must be running.
- `./gradlew bootJar`: package the application as `build/libs/app.jar`.
- `./gradlew test --tests '*CustomerApiIntegrationTest'`: run a focused API integration check while developing.

Extend the `web`, `service`, `domain`, and `repository` packages under your chosen
Java package in `src/main/java`. Add database changes as new versioned SQL files in
`src/main/resources/db/migration`; keep migrations that have already been applied
unchanged. Keep configuration in `src/main/resources/application.yaml`, with
credentials supplied through the environment. Add API and persistence coverage
under `src/test/java`, using the existing customer tests as examples.

## Disposable smoke checks

Run one of these checks from a disposable generated copy, with no development
application running:

- `./scripts/dev-smoke-test.sh`: start `bootRun`, check health and the seeded API, then stop it.
- `./scripts/smoke-test.sh`: build and start the packaged container, then check health and the seeded API.

Both scripts run Compose `down -v` before and after the check, removing containers
and database volumes. They are not the everyday development startup command and
can delete existing local data for this Compose project.

## Local container workflow

To run the application and PostgreSQL entirely in containers, use this alternative
to `bootRun`. Create `.env` as above if this is a fresh copy. Switching Compose
configurations can recreate the database container; use a separate generated copy
if you need to preserve your `bootRun` database.

```bash
docker compose -f docker-compose.yml --env-file .env up --build
```

Press Ctrl-C to stop the containers and keep local data. Restart with the same
command. Default host ports are `8080` for the app, `8081` for management, and
`5432` for PostgreSQL. Override `APP_HOST_PORT`, `MANAGEMENT_HOST_PORT`, or
`POSTGRES_EXPORT_PORT` in `.env` if those ports are already in use locally.

## HTTP API
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`

## Error contract

Application errors use RFC `9457` problem details with:
- `type`
- `title`
- `status`
- `detail`
- `instance`
- `errors` for validation failures only

The starter seeds one customer record through Flyway so health and API checks have immediate data.

## Observability

Prometheus metrics are exposed at `/actuator/prometheus` on the management port. Spring Boot's OpenTelemetry starter enables request tracing and adds `traceId` and `spanId` to logs written within a trace. The default sampling probability is 10%; tune it with `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` (from `0.0` to `1.0`).

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
