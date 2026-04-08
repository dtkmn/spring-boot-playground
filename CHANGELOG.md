# Changelog

All notable changes to this repository should be documented in this file.

The format follows a simple keep-a-changelog style with concise user-visible entries.

## [Unreleased]

### Added
- Formal release process and stabilization rules for the starter repository
- Pilot playbook, pilot feedback template, and release readiness checklist
- `compose.yaml` development-services path and `dev-smoke-test.sh` for generated starters
- RFC 9457 problem-details error contract for both starter variants
- OTLP-ready tracing dependencies, log correlation support, and opt-in structured logging profile
- Optional Helm values for service accounts, pod security context, scheduling, and disruption budgets

### Changed
- Reposition repository from playground to Spring service starter
- Add starter variants, examples scaffolding, bootstrap script, and Helm chart
- Upgrade starter and example baseline to Spring Boot 3.5.13 and align Docker builder images on Gradle 9.4.0
- Move starter integration tests to Spring Boot Testcontainers service connections
