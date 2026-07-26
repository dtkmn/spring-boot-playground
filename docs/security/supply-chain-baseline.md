# Supply Chain Baseline

## Contract

Generated services must give teams a reasonable starting point for dependency and build-chain visibility.

The starter now includes:

- Dependabot coverage for Gradle, GitHub Actions, and Docker base images
- CycloneDX SBOM generation through the Gradle `check` lifecycle
- CI upload of test, coverage, and SBOM reports
- digest-pinned, non-root distroless Java runtime containers by default
- built-image validation for runtime user, Java entrypoint, command, and shell absence
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
- Packaged-container smoke tests must run `scripts/validate-container-image.sh` against the built application image.
- Runtime images must keep an effective non-root user and must not contain `/bin/sh`.
- Dependabot must cover Gradle, GitHub Actions, and Docker files.

## Runtime Exceptions

Distroless is the default, not a blind mandate. A service may use another approved runtime when it genuinely requires OS packages, native libraries, fonts, shell execution, or unsupported diagnostics.

An exception must document the missing capability, owner, compensating controls, patching policy, and dedicated container smoke coverage. Convenience and habitual `docker exec` debugging are not sufficient reasons.
