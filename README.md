# Spring Service Starter

`spring-boot-playground` is being repurposed into an opinionated Spring service starter for internal engineering teams.

The default path is `mvc-jpa`. `webflux-r2dbc` remains supported for teams with a real reactive requirement. Kafka, Kafka Streams, Binance/websocket ingestion, and other integration-heavy assets are being isolated into `examples/` so the starter contract stays narrow and production-biased.

## Starter Contract

What every starter variant should provide:
- Java 21 and Spring Boot 3.x
- PostgreSQL plus Flyway
- Actuator and Prometheus metrics
- Docker and Docker Compose for local development
- Test baseline suitable for CI
- Kubernetes deployment support through Helm

What is not part of the default starter contract:
- Kafka and Kafka Streams
- Websocket ingest pipelines
- Domain-specific integrations
- Broad demo code mixed into the main runtime path

## Repository Layout

- `variants/mvc-jpa`: default starter and recommended path for most new services
- `variants/webflux-r2dbc`: supported advanced variant for reactive workloads
- `examples/`: isolated integration examples outside the starter contract
- `deploy/helm/spring-service-starter`: shared Kubernetes deployment chart
- `docs/adr`: architecture decision records
- `scripts/init-service.sh`: starter bootstrap script

## Recommended Starting Path

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

## Variants

### `mvc-jpa`
Use this when:
- the service is request/response heavy
- the team wants the lowest-friction Spring path
- JPA and standard blocking I/O are acceptable

Includes:
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Actuator and Prometheus
- Docker and Compose

### `webflux-r2dbc`
Use this when:
- the service has a real reactive requirement
- the team is comfortable with Reactor and reactive persistence
- non-blocking I/O provides clear value

Includes:
- Spring WebFlux
- Spring Data R2DBC
- PostgreSQL
- Flyway
- Actuator and Prometheus
- Docker and Compose

## Examples

Examples are intentionally outside the starter contract.

Current example tracks:
- `examples/kafka-basic`
- `examples/kafka-streams`
- `examples/binance-websocket`

Each example owns its own README and runtime assets. Engineers should not clone an example into a new service unless that integration is actually required.

## Kubernetes And Helm

The shared deployment path lives in `deploy/helm/spring-service-starter`.

Supported chart contract:
- `image.repository`
- `image.tag`
- `service.port`
- `env`
- `secrets.existingSecret`
- `resources`
- `autoscaling.enabled`
- `ingress.enabled`
- `ingress.hosts`
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

## Governance

- `SUPPORT.md`: support window and update cadence
- `CHANGELOG.md`: release history
- `CONTRIBUTING.md`: contribution and review rules
- `docs/adr`: architectural decisions

## Transitional Note

The root application in this repository still contains legacy playground code while the roadmap issues split it into starter variants and examples. The source of truth for new services is the variant/template structure introduced here, not the legacy root runtime.
