# Release Readiness Checklist

## Purpose

Use this checklist before creating a release tag from `main`.

If the release is tracked in GitHub, use a milestone, project item, or release tracking issue. Do not hard-code local issue numbers into this document. A checklist that only works in one maintainer's repository view is not release governance; it is private bookkeeping.

## Blunt Usability Check

The previous checklist was too close to theater. It named good intentions, but it depended on phantom issue references and did not show a maintainer how to prove the gates passed.

This checklist is usable only when every checked item has evidence: a CI run, command output, release review link, pilot feedback link, or a documented exception with an owner and follow-up. If those artifacts do not exist, the release is not ready.

## Entry Criteria

- The planned release scope is written in `CHANGELOG.md` under `Unreleased`.
- Pilot evidence exists in linked `Pilot Feedback` issues, a release milestone, or another team-visible tracker.
- `dev` is green in both repository validation and starter validation workflows.
- `main` is reserved for stabilized release promotions only.
- Any release-blocking pilot findings have either been fixed or explicitly deferred with an owner.

## Readiness Gates

### Product

- `mvc-jpa` is still the documented default path in `README.md`, `variants/README.md`, and `docs/adoption/promotion-brief.md`.
- `webflux-r2dbc` is still explicitly marked as the advanced variant.
- Optional integrations remain isolated under `examples/` and are not presented as starter defaults.
- Pilot feedback does not show teams rewriting the generated structure immediately after generation.

### Quality

- Repository validation passes on `dev`:

```bash
./.github/scripts/validate-repository.sh
```

- Starter builds pass locally or in CI:

```bash
./gradlew -p variants/mvc-jpa/template check --no-daemon
./gradlew -p variants/webflux-r2dbc/template check --no-daemon
```

- The `Starter Validation` workflow passes for both `mvc-jpa` and `webflux-r2dbc`.
- Generated starters pass build, Docker Compose config validation, development smoke test, packaged-container smoke test, Helm lint, and Helm template.
- Generated starters produce test, JaCoCo, and CycloneDX report output under `generated/<artifact>/build/reports/`.
- Generated runtime Docker images still run as a non-root user.

### Governance

- `CHANGELOG.md` reflects the release scope.
- `SUPPORT.md` and `RELEASING.md` are current.
- `docs/adoption/promotion-brief.md` reflects current adoption status.
- `docs/releases/version-policy.md` reflects the current Java, Spring Boot, and Gradle baselines.
- `docs/security/supply-chain-baseline.md` reflects current generated-service supply-chain gates.
- The publish contract remains tag-gated for generated services.
- No release or promotion doc contains maintainer-local absolute paths.

## Release Steps

1. Promote the release candidate from `dev` to `main`.
2. Verify validation workflows on `main`.
3. Update `CHANGELOG.md` with a dated release section.
4. Create and push the annotated tag from the `main` commit:

```bash
: "${VERSION:?Set VERSION to the release tag, for example v1.0.0}"
git checkout main
git pull --ff-only
git tag -a "$VERSION" -m "Spring Service Starter $VERSION"
git push origin "$VERSION"
```

5. Create the GitHub release using the changelog summary.
6. Link the release to the milestone, project item, or release tracking issue that contains the evidence above.

## Post-Release Backlog

- Open or update the next-release backlog tracker.
- Link pilot follow-up issues that did not block the current release.
- Separate:
  - must-fix before wider adoption
  - safe for next minor release
  - examples-only improvements

## Exit Criteria

- The release tag exists on GitHub.
- The GitHub release is published.
- The post-release backlog tracker is linked from the release notes or release tracking issue.
