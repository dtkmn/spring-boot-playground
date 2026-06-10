# ADR-003: 2026 Modernization Tranches

## Status
Accepted

## Context
The starter repository needs modernization without breaking its internal golden path through an unmanaged jump to Spring Boot 4. The immediate gaps are dependency drift, variant contract drift, outdated local development conventions, and insufficient deployment hardening.

## Decision
- Stabilize the repository on the latest maintained Spring Boot `3.5.x` line first.
- Standardize both starter variants on RFC `9457` problem details before attempting a major-version migration.
- Make `compose.yaml` plus Spring Boot development services the preferred `bootRun` workflow while keeping `docker-compose.yml` for standalone container smoke validation.
- Keep `mvc-jpa` as the golden path and `webflux-r2dbc` as the advanced supported variant.
- Defer API versioning design until the dedicated Spring Boot 4 tranche.

## Consequences
- Generated services inherit a consistent API error contract across both variants.
- Local development and CI validation both cover the `bootRun` path and the full container path.
- Boot 4 work remains an explicit follow-up tranche rather than an unbounded refactor folded into routine maintenance.
