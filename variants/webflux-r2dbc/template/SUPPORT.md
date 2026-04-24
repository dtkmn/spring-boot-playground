# Support Policy

## Baseline
- Java 21
- Spring Boot 3.5.x stable line
- PostgreSQL
- Kubernetes + Helm deployment path

## Update Cadence
- Quarterly dependency refreshes
- Out-of-band security and critical regression fixes

## Release Contract
- CI validates `./gradlew check`, Docker Compose, and Helm rendering
- CI uploads test, coverage, and CycloneDX SBOM reports
- Runtime Docker images run as a non-root user by default
- `v*` git tags publish Docker images to GHCR
