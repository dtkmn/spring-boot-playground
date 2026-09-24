# Changelog

All notable changes to this repository should be documented in this file.

The format follows a simple keep-a-changelog style with concise user-visible entries.

## [Unreleased]

### Added
- Generated `STARTER.md` records source provenance and regeneration options, with a manual upgrade guide for existing services
- MIT licensing for the starter and copied scaffold, with the license included in newly generated services

### Changed
- Remove the generator's Perl dependency by using Bash's built-in placeholder replacement
- Clarify the public starter's purpose and contributor entry point
- Document a complete first run and separate everyday development from disposable smoke checks
- Define latest-stable maintenance, patch cadence, and application owners' responsibility for dependencies and copied starter assets

## [1.0.0] - 2026-09-24

First versioned release of the Spring service starter, with MVC/JPA as the default and WebFlux/R2DBC as the advanced variant.

### Added
- Formal release process and stabilization rules for the starter repository
- Pilot playbook, pilot feedback template, and release readiness checklist
- `compose.yaml` development-services path and `dev-smoke-test.sh` for generated starters
- RFC 9457 problem-details error contract for both starter variants
- OTLP-ready tracing dependencies, log correlation support, and opt-in structured logging profile
- Optional Helm values for service accounts, pod security context, scheduling, and disruption budgets
- Promotion brief, version policy, and supply-chain baseline docs for controlled internal adoption
- CycloneDX SBOM generation and JaCoCo coverage reports for generated starters
- Repository validation for starter promotion contract requirements

### Changed
- Remove duplicate starter dependencies and redundant Kafka listener/serde configuration; keep template wrapper files centralized at the repository root
- Reposition repository from playground to Spring service starter
- Add starter variants, examples scaffolding, bootstrap script, and Helm chart
- Upgrade the starter and example baseline to Spring Boot 4.1.1, upgrade CycloneDX to 3.4.1, and align the Gradle wrapper and Docker builder images on Gradle 9.7.1
- Move starter integration tests to Spring Boot Testcontainers service connections
- Run generated starter CI and publish workflows through `./gradlew check`
- Replace starter and example Alpine runtimes with a digest-pinned, non-root distroless Java image

### Fixed
- Set up Docker Buildx before publishing generated service images so GitHub Actions build caching has a compatible builder
- Keep local build output, caches, IDE files, and `.env` out of generated services, and preserve binary assets during placeholder replacement
- Complete Kafka Streams startup on Boot 4 with Jackson 3 and Boot-managed Kafka dependencies, and add context and trade-aggregation regression tests
- Analyze each Gradle project separately with CodeQL to avoid failed root dependency discovery and duplicate template class exclusions
- Restore Spring Boot 4 tracing auto-configuration and log correlation in both starter variants, with documented opt-in OTLP export and no collector required by default
