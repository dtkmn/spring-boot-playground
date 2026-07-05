# Support Policy

## Audience

This starter is optimized for internal engineering teams and is currently an internal starter candidate.

Wider rollout requires the adoption gates in `docs/adoption/promotion-brief.md`.

## Update Cadence

- Quarterly stable dependency and framework updates
- Out-of-band patches only for security issues or critical regressions

## Release Channel

- `dev`: active integration branch
- `main`: stabilized release branch
- `v*` tags: immutable release points

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

## Supply Chain Baseline

- Generated starters produce CycloneDX SBOM output during `./gradlew check`.
- Generated starter CI uploads test, coverage, and SBOM reports.
- Generated Dockerfiles run application containers as a non-root user.
- Dependabot tracks Gradle, GitHub Actions, and Docker base images.
