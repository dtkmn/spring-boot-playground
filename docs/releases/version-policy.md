# Version Policy

## Purpose

This document keeps the starter from drifting into accidental legacy. Dependency movement should be intentional, tested, and tied to the starter contract.

## Current Baseline

As of September 2, 2026:

| Layer | Default | Status |
| --- | --- | --- |
| Java | 21 | Stable starter baseline |
| Spring Boot | 4.1.1 | Stable starter baseline |
| Gradle | 9.7.1 | Current wrapper and Docker builder baseline |
| PostgreSQL | 17 | Local and integration-test baseline |
| Runtime image | Distroless Java 21 on Debian 13 | Digest-pinned, non-root baseline |
| Java 25 | Future migration tranche | Not the default yet |

References:

- Spring Boot 4.1.1 was released on August 20, 2026: https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/
- Oracle announced Java 25 on September 16, 2025 with long-term support: https://www.oracle.com/news/announcement/oracle-releases-java-25-2025-09-16
- Gradle 9.7.1 was released on August 19, 2026: https://docs.gradle.org/9.7.1/release-notes.html

## Baseline Rules

- Patch updates on the current stable line should be applied promptly when generated starters remain green.
- Minor or major platform changes require generated starter validation, not a casual version bump.
- Java baseline changes require generated service tests, smoke tests, Docker builds, and Helm rendering to pass.
- Spring Boot major-version changes require migration notes and generated starter validation.
- Gradle wrapper and Docker builder image versions should stay aligned.
- Distroless runtime digest updates require generated container smoke validation.
- Examples should follow the starter baseline unless an example has a documented integration constraint.

## Stable Line

Maintenance targets the latest stable starter release. Older tags are snapshots, with no routine backports or maintained historical branches; exceptions require an explicit maintainer decision. See [Support Policy](../../SUPPORT.md) for scope and downstream ownership.

The stable line is Java 21 and Spring Boot 4.1.1. It remains stable only while:

- generated `mvc-jpa` and `webflux-r2dbc` services compile and test cleanly
- Docker images build and run
- generated images pass packaged-container smoke tests on the pinned non-root distroless runtime
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
- Plan broader dependency and platform refreshes quarterly; compatible patch updates should not wait for that cycle.
- Prioritize security fixes and critical regressions promptly, with generated starter validation before release.
