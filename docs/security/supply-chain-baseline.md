# Supply Chain Baseline

## Contract

Generated services must give teams a reasonable starting point for dependency and build-chain visibility.

The starter now includes:

- Dependabot coverage for Gradle, GitHub Actions, and Docker base images
- CycloneDX SBOM generation through the Gradle `check` lifecycle
- CI upload of test, coverage, and SBOM reports
- non-root runtime containers by default
- tag-gated image publishing for generated services

## What This Does Not Solve

This is not a full security program. It does not replace:

- threat modeling
- runtime vulnerability scanning
- container signing
- artifact attestation
- authorization design
- secrets management policy
- production admission controls

Pretending otherwise would be sloppy. This baseline improves visibility and default posture; platform security still needs explicit ownership.

## Scanner Policy

Do not add a mutable third-party scanner action casually. In March 2026, the Trivy GitHub Action ecosystem was compromised through force-pushed tags and malicious releases. That incident is a useful warning: security tooling can become the attack path.

If a vulnerability scanner is added to generated workflows, require:

- pinned action references or an internally approved action mirror
- documented update ownership
- no broad write permissions
- no secret exposure on pull-request workflows
- generated starter validation before rollout

## Required Evidence

Before a release:

- `./gradlew check` must generate SBOM output for each generated starter.
- Generated CI must upload SBOM and coverage reports.
- Dockerfiles must keep a non-root runtime user.
- Dependabot must cover Gradle, GitHub Actions, and Docker files.
