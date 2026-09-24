# Support Policy

The application maintainer defines this service's support lifetime, release cadence, and response commitments. The source starter's support policy does not provide a support commitment for this generated application.

## Baseline
- Java 21
- Spring Boot 4.1.x stable line
- PostgreSQL
- Kubernetes + Helm deployment path

## Update Ownership
- Maintain dependencies and copied application code, build files, workflows, scripts, and Helm assets.
- Review source starter release notes for relevant changes; this service is an independent copy and receives no automatic source updates. Dependabot updates dependencies, not copied starter assets.
- Plan broader dependency and framework refreshes quarterly, apply compatible patches promptly, and prioritize security and critical regression fixes. Validate updates before releasing the application.

## Release Contract
- CI validates `./gradlew check`, Docker Compose, and Helm rendering
- CI uploads test, coverage, and CycloneDX SBOM reports
- Runtime Docker images use a digest-pinned distroless Java base and run as non-root
- `v*` git tags publish Docker images to GHCR
