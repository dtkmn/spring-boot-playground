# ADR-003: 2026 Modernization Tranches

## Status
Superseded

## Context
The starter repository needed modernization without breaking its internal golden path through an unmanaged jump to Spring Boot 4. The immediate gaps were dependency drift, variant contract drift, outdated local development conventions, and insufficient deployment hardening.

## Decision
- Stabilized the repository on the maintained Spring Boot `3.5.x` line first.
- Standardized both starter variants on RFC `9457` problem details before attempting a major-version migration.
- Make `compose.yaml` plus Spring Boot development services the preferred `bootRun` workflow while keeping `docker-compose.yml` for standalone container smoke validation.
- Keep `mvc-jpa` as the golden path and `webflux-r2dbc` as the advanced supported variant.
- Deferred API versioning design until after the Spring Boot 4 tranche.

## Consequences
- Generated services inherit a consistent API error contract across both variants.
- Local development and CI validation both cover the `bootRun` path and the full container path.
- Boot 4 work moved into the current baseline after the dedicated Spring Boot 4.1 migration work began.
