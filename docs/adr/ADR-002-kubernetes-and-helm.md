# ADR-002: Kubernetes And Helm As The Cloud Deployment Path

## Status
Accepted

## Context
The starter needs a first-class cloud deployment story. Supporting every cloud or orchestration model in v1 would create unnecessary surface area.

## Decision
- Kubernetes plus Helm is the only first-class deployment path in this roadmap window.
- The chart assumes external PostgreSQL and does not bundle Kafka for starter services.
- Health probes are wired to Spring Boot Actuator endpoints.

## Consequences
- Deployment guidance stays portable and operationally standard.
- Teams using other platforms can adapt from the container baseline, but Kubernetes plus Helm is the supported path.
