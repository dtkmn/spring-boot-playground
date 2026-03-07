# __SERVICE_NAME__

Spring MVC + JPA starter service generated from the Spring Service Starter repository.

## Stack
- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Actuator + Prometheus
- Docker + Docker Compose
- Helm for Kubernetes deployment

## Local development

```bash
cp .env.example .env
./gradlew test
./gradlew bootRun
./scripts/smoke-test.sh
```

## Local container workflow

```bash
cp .env.example .env
docker compose --env-file .env up --build
```

Default ports:
- app: `__APP_PORT__`
- management: `__MANAGEMENT_PORT__`
- PostgreSQL export: `5432`

## HTTP API
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`

## Error contract

Application errors return a stable JSON envelope with:
- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `validationErrors`

The starter seeds one customer record through Flyway so health and API checks have immediate data.

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

## Release workflow

Create and push a `v*` tag to publish the container image to GHCR:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Release tags must be created from commits already promoted to `main`. See `RELEASING.md` for the full release checklist.
