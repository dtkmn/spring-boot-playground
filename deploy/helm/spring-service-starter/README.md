# spring-service-starter Helm Chart

This chart is the supported Kubernetes deployment path for starter-generated services.

Expected contract:
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

Defaults:
- external PostgreSQL
- no bundled Kafka
- Actuator-backed liveness, readiness, and startup probes
