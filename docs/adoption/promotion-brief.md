# Spring Service Starter Promotion Brief

## Status

The starter is being prepared for v1.0. Both variants have passed generated-service trials covering tests, Docker Compose, Kubernetes deployment, API behavior, and persistence after restart.

These checks establish a working baseline. Each adopting service still needs its own production configuration and operational review.

## Positioning

Use this starter when a team needs a conventional Spring service with:

- Spring MVC or WebFlux
- PostgreSQL
- Flyway-managed schema changes
- Docker Compose local development
- Testcontainers-backed integration tests
- Actuator health, metrics, and tracing hooks
- Helm deployment scaffolding
- CI, image publishing, Dependabot, SBOM, and release docs

Service-specific responsibilities include:

- authentication and authorization
- domain architecture and optional integrations
- production infrastructure and operations

## Promotion Message

> This starter provides a repeatable Spring Boot 4.1 and Java 21 service baseline with local development, tests, container packaging, Helm scaffolding, and release workflows. Choose MVC/JPA by default or WebFlux/R2DBC for a reactive service, then add your domain features.

## Adoption Rules

- `mvc-jpa` is the default path.
- `webflux-r2dbc` requires an explicit reactive requirement.
- Examples are recipes, not starter contract.
- Teams may add Kafka, websocket ingestion, or other integrations after generation, but those choices must not move into the default starter without adoption evidence.
- A generated service can be evaluated locally before creating a separate repository. Record changes needed for adoption so recurring gaps can be fixed in the starter.

## Validation Before Release

Follow the [release readiness checklist](../releases/release-readiness-checklist.md). In particular:

1. Both generated variants pass `check`, Docker Compose validation, smoke tests, and Helm rendering in CI.
2. Generated services produce coverage and SBOM artifacts through CI.
3. Container images use the digest-pinned, non-root distroless runtime, with any exception documented and tested.
4. Validation findings and known dependency advisories are reviewed before release.

## Adoption Feedback

Use the [pilot playbook](pilot-playbook.md) for a local generated-service trial or an adopting service. Summarize results and accepted limitations in the release or pilot tracker; detailed logs may remain private.

Revisit the defaults when adoption reveals:

- repeated structural rewrites after generation
- excessive setup time or manual CI fixes
- deployment failures in the intended environment
- optional integrations displacing the simple default path

Java 25 and additional integrations are future work, independent of v1.0 readiness. Baseline changes follow the [version policy](../releases/version-policy.md).
