# Spring Service Starter Promotion Brief

## Status

This repository should be promoted as an internal starter candidate, not as a finished platform product.

That distinction matters. A starter candidate is good enough for controlled adoption with feedback loops. A platform product must already have proven support load, upgrade paths, security posture, and production evidence. This repo is not there yet.

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

Do not sell it as:

- a complete platform
- an authorization framework
- an eventing framework
- a service mesh strategy
- a Java runtime migration solution
- a domain architecture template

## Promotion Message

The honest pitch:

> This starter gives teams a repeatable Spring Boot 4.1 service baseline with working local development, tests, container packaging, Helm scaffolding, and release hygiene. It is ready for pilot services. Wider adoption depends on pilot evidence and a later Java 25 modernization tranche.

## Adoption Rules

- `mvc-jpa` is the default path.
- `webflux-r2dbc` requires an explicit reactive requirement.
- Examples are recipes, not starter contract.
- Teams may add Kafka, websocket ingestion, or other integrations after generation, but those choices must not move into the default starter without adoption evidence.
- Generated services should be committed to their own repositories without structural rewrites during the pilot. If every team rewrites the scaffold immediately, the starter has failed.

## Promotion Gates

Before this becomes the default starter for more than pilot teams:

1. Two generated pilot services must reach a real deployment environment.
2. Pilot teams must report setup time, changes made after generation, and missing defaults.
3. Generated `mvc-jpa` and `webflux-r2dbc` services must pass `check`, Docker Compose validation, smoke tests, and Helm rendering in CI.
4. Generated services must publish SBOM artifacts through the CI workflow.
5. Container images must run as a non-root user by default.
6. Java 25 must produce at least one green generated service before the Java baseline changes.
7. Security posture must be explicit: either a starter security baseline is added or the repo documents why service-level auth remains outside the starter contract.

## Evidence Contract

Promotion evidence must be visible to the team reviewing the release: pilot feedback links, generated service repositories, CI runs, deployment notes, and documented exceptions. Do not cite local paths, private issue numbers, or implied tribal knowledge as proof.

## Kill Criteria

Stop promoting this starter if:

- pilot teams remove most of the generated structure
- local setup takes more than one hour for a typical service
- generated services require manual CI surgery
- the Helm chart cannot deploy unchanged to the target cluster baseline
- the default path becomes a dumping ground for optional integrations
- version drift is handled by drive-by dependency bumps instead of the version policy

## Next-Level Work

Priority order:

1. Finish pilot adoption and collect evidence.
2. Add an explicit security/auth stance.
3. Run the Java 25 modernization tranche when pilot evidence supports it.
4. Add OpenAPI generation once the API shape is stable.
5. Add stricter code quality gates after pilot feedback confirms they will not create busywork.
