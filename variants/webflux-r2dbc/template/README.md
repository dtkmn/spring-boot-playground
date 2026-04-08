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
- Helm for Kubernetes deployment

## Local development

```bash
cp .env.example .env
./gradlew test
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

- Prometheus metrics remain exposed through Actuator
- OTLP tracing can be enabled by setting `MANAGEMENT_OTLP_TRACING_ENDPOINT`
- Structured JSON console logs are opt-in through the `structured-logging` profile

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
