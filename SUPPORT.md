# Support Policy

## Audience

This starter supports developers and teams building Spring services. Its supported scope covers the generator, starter variants, local development assets, and shared Helm chart.

Both variants are validated using generated services with PostgreSQL, Docker Compose, and local Kubernetes. See [adoption guidance](docs/adoption/promotion-brief.md) for checks in a service's target environment.

## Update Cadence

- Plan dependency and framework refreshes quarterly.
- Apply compatible patches promptly after generated-service validation.
- Prioritize security fixes and critical regressions outside the planned refresh cycle, with the same validation requirements.

## Release Channel

- `dev`: active integration branch
- `main`: stabilized release branch
- `v*` tags: immutable release points

Maintenance targets the latest stable starter release. Older releases remain available as snapshots; historical release branches are not routinely maintained. Backports require an explicit maintainer decision for the affected release and fix.

## Compatibility Baseline

- Java 21
- Spring Boot 4.1.x maintained baseline line
- PostgreSQL as the default persistence contract
- Kubernetes plus Helm as the supported cloud deployment path

See `docs/releases/version-policy.md` for baseline and migration rules.

## Support Levels

- `mvc-jpa`: primary supported path
- `webflux-r2dbc`: supported advanced variant
- `examples/`: reference only, best-effort support

Consuming applications own their authentication, authorization, secrets, and production configuration.

Application maintainers also own dependency upgrades and updates to copied application code, build files, workflows, scripts, and Helm assets. Generating a service creates an independent copy: starter releases do not update it automatically, and Dependabot does not synchronize these copied assets. Review starter release notes and apply relevant changes with the application's own validation.

## Supply Chain Baseline

- Generated starters produce CycloneDX SBOM output during `./gradlew check`.
- Generated starter CI uploads test, coverage, and SBOM reports.
- Generated Dockerfiles use a digest-pinned distroless Java runtime.
- Dependabot tracks Gradle, GitHub Actions, and Docker base images.
