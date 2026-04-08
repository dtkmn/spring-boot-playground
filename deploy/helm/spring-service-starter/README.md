# spring-service-starter Helm Chart

This chart is the supported Kubernetes deployment path for starter-generated services.

Expected contract:
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

Defaults:
- external PostgreSQL
- no bundled Kafka
- Actuator-backed liveness, readiness, and startup probes
- optional service-account, scheduling, and disruption-budget hardening

## Secret Contract

When `secrets.existingSecret` is set, the deployment reads all key/value pairs from that secret through `envFrom`.

The starter variants expect these database secrets:
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

Optional application secrets can be added to the same secret.

## Example

```bash
kubectl create secret generic customer-profile-db \
  --from-literal=POSTGRES_USER=app \
  --from-literal=POSTGRES_PASSWORD=change-me

helm upgrade --install customer-profile deploy/helm/spring-service-starter \
  -f deploy/helm/spring-service-starter/values-dev.yaml \
  --set image.repository=ghcr.io/example-org/customer-profile \
  --set image.tag=1.0.0 \
  --set secrets.existingSecret=customer-profile-db \
  --set postgres.host=postgres.dev.svc.cluster.local \
  --set postgres.database=customer_profile
```

The same chart contract is intended to work for both `mvc-jpa` and `webflux-r2dbc` services.
