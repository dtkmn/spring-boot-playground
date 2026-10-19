# Support Policy

## Audience

This starter supports developers and teams building Spring services. Its maintained scope covers API generation, both API variants, local development assets, and the shared Helm chart. MVC/JPA AI generation is experimental and scoped to host and local Docker Compose use.

Both variants are validated using generated services with PostgreSQL, Docker Compose, and local Kubernetes. See [adoption guidance](docs/adoption/promotion-brief.md) for checks in a service's target environment.

## Update Cadence

- Review dependency alerts, update pull requests, and CI failures weekly.
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
- Kubernetes plus Helm as the supported cloud deployment path for maintained API output

See `docs/releases/version-policy.md` for baseline and migration rules.

## Support Levels

- `mvc-jpa` / `--purpose api`: primary supported path
- `webflux-r2dbc` / `--purpose api`: supported advanced variant
- `mvc-jpa` / `--purpose ai`: experimental, best-effort host/Compose support; no Helm or production identity commitment
- `examples/`: reference only, best-effort support

The AI output uses one Ollama classification call followed by Java-owned,
owner-scoped read-only lookups. Its two-token authentication is a local fixture.
Model requests are opt-in; regular checks use a fake model endpoint. Other model
choices require their own live evaluation, and passing sample questions does not
establish general language accuracy. No response-time or model-availability
commitment is provided.

Consuming applications own their authentication, authorization, secrets, and production configuration.

Application maintainers also own dependency upgrades and updates to copied application code, build files, workflows, scripts, and Helm assets. Generating a service creates an independent copy: starter releases do not update it automatically, and Dependabot does not synchronize these copied assets. Review starter release notes and apply relevant changes with the application's own validation.

## Supply Chain Baseline

- Generated starters produce CycloneDX SBOM output during `./gradlew check`.
- Generated starter CI uploads test, coverage, and SBOM reports.
- Generated Dockerfiles use a digest-pinned distroless Java runtime.
- Dependabot tracks Gradle, GitHub Actions, and Docker base images.
