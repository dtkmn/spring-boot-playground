# ADR-004: Promotion And Supply Chain Baseline

## Status
Accepted

## Context

The starter is strong enough for controlled adoption but not mature enough to be marketed as a complete platform product. The biggest adoption risks are hidden dependency drift, weak generated-service evidence, and unclear security ownership.

## Decision

- Promote the repository as an internal starter candidate.
- Keep `mvc-jpa` as the default adoption path.
- Require generated starters to run `./gradlew check` in CI and release workflows.
- Generate CycloneDX SBOM output and JaCoCo coverage reports for generated services.
- Upload generated-service quality reports from CI.
- Run generated Docker images as a non-root user by default.
- Track Gradle, GitHub Actions, and Docker updates through Dependabot.
- Keep third-party vulnerability scanner actions out of the default workflow until an approved pinning and ownership policy exists.

## Consequences

- Promotion claims are backed by generated output, not only documentation.
- Teams get better build-chain visibility on day one.
- The repo remains honest about what it does not solve: authorization, runtime scanning, artifact signing, and production security controls still require explicit platform ownership.
- Java 25 remains a separate modernization tranche rather than an accidental breaking change folded into promotion work.
