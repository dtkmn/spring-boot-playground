# Version Policy

## Purpose

This document keeps the starter from drifting into accidental legacy. Dependency movement should be intentional, tested, and tied to the starter contract.

## Current Baseline

As of July 5, 2026:

| Layer | Default | Status |
| --- | --- | --- |
| Java | 21 | Stable starter baseline |
| Spring Boot | 4.1.0 | Stable starter baseline |
| Gradle | 9.6.1 | Current wrapper and Docker builder baseline |
| PostgreSQL | 17 | Local and integration-test baseline |
| Java 25 | Future migration tranche | Not the default yet |

References:

- Spring Boot 4.1.0 was released on June 10, 2026: https://spring.io/blog/2026/06/10/spring-boot-4
- Oracle announced Java 25 on September 16, 2025 with long-term support: https://www.oracle.com/news/announcement/oracle-releases-java-25-2025-09-16
- Gradle 9.6.1 was published on June 26, 2026: https://gradle.org/releases

## Baseline Rules

- Patch updates on the current stable line should be applied promptly when generated starters remain green.
- Minor or major platform changes require generated starter validation, not a casual version bump.
- Java baseline changes require generated service tests, smoke tests, Docker builds, and Helm rendering to pass.
- Spring Boot major-version changes require migration notes and generated starter validation.
- Gradle wrapper and Docker builder image versions should stay aligned.
- Examples should follow the starter baseline unless an example has a documented integration constraint.

## Stable Line

The stable line is Java 21 and Spring Boot 4.1.0. It remains stable only while:

- generated `mvc-jpa` and `webflux-r2dbc` services compile and test cleanly
- Docker images build and run
- the Helm chart renders without compatibility changes
- RFC 9457 problem details still behave consistently
- Flyway, Testcontainers, Actuator, Micrometer, and OTLP hooks remain compatible
- pilot feedback does not expose unacceptable migration cost

## Next Line

The next line is Java 25 on the Spring Boot 4.x baseline.

The Java 25 migration should be treated as a product decision, not a dependency chore. It should produce migration notes for generated services, call out breaking changes, and define whether Java 25 becomes required or optional.

## Dependency Review Rules

- Generated services must keep Dependabot enabled for Gradle, GitHub Actions, and Docker.
- Generated services must produce a CycloneDX SBOM during CI.
- Dependency updates that affect the starter contract require generated output validation, not just root repository validation.
- Security fixes may bypass the quarterly cadence, but they still need generated starter validation before release.
