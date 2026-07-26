# Support Policy

## Baseline
- Java 21
- Spring Boot 4.1.x stable line
- PostgreSQL
- Kubernetes + Helm deployment path

## Update Cadence
- Quarterly dependency refreshes
- Out-of-band security and critical regression fixes

## Release Contract
- CI validates `./gradlew check`, Docker Compose, and Helm rendering
- CI uploads test, coverage, and CycloneDX SBOM reports
- Runtime Docker images use a digest-pinned distroless Java base and run as non-root
- Container smoke tests validate the effective runtime-image contract
- `v*` git tags publish Docker images to GHCR
