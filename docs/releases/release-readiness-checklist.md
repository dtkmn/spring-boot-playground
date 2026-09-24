# Release Readiness Checklist

## Purpose

Use this checklist before creating a release tag from `main`.

Record the release decision and validation summary in a release tracking issue, milestone, or project item. Use CI runs and trial results as evidence; detailed local logs may remain private.

## Entry Criteria

- `CHANGELOG.md` describes the planned release scope.
- Both variants have accepted generated-service trial results, from local validation or service adoption.
- Repository Validation, Starter Validation, and CodeQL are green on `dev`.
- `main` is reserved for stabilized release promotions only.
- Blocking findings are fixed; accepted limitations and known dependency advisories are recorded with their scope and follow-up.

## Readiness Gates

### Product

- `mvc-jpa` is still the documented default path in `README.md`, `variants/README.md`, and `docs/adoption/promotion-brief.md`.
- `webflux-r2dbc` is still explicitly marked as the advanced variant.
- Optional integrations remain isolated under `examples/` and are not presented as starter defaults.
- Generated-service trials show no unresolved blocker in setup, API behavior, deployment, or persistence after restart.

### Quality

- Repository validation passes on `dev`:

```bash
./.github/scripts/validate-repository.sh
```

- Starter builds pass locally or in CI. Generate fresh services first; raw templates contain placeholders. Run from the repository root, using output directories that do not already exist:

```bash
for variant in mvc-jpa webflux-r2dbc; do
  artifact="release-check-$variant"
  ./scripts/init-service.sh \
    --variant "$variant" \
    --service-name "$artifact" \
    --group-id tech.example \
    --artifact-id "$artifact" \
    --package-name tech.example.releasecheck || exit 1
  ./gradlew -p "generated/$artifact" check --no-daemon || exit 1
done
```

- The `Starter Validation` workflow passes for both `mvc-jpa` and `webflux-r2dbc`.
- Generated starters pass build, Docker Compose config validation, development smoke test, packaged-container smoke test, Helm lint, and Helm template.
- Generated starters produce test, JaCoCo, and CycloneDX report output under `generated/<artifact>/build/reports/`.
- Generated runtime Docker images use the pinned non-root distroless base and pass packaged-container smoke tests.
- Any service that cannot use the default distroless runtime has a reviewed exception and dedicated runtime smoke coverage.

### Governance

- `CHANGELOG.md` reflects the release scope.
- `SUPPORT.md` and `RELEASING.md` are current.
- `docs/adoption/promotion-brief.md` reflects current adoption status.
- `docs/releases/version-policy.md` reflects the current Java, Spring Boot, and Gradle baselines.
- `docs/security/supply-chain-baseline.md` reflects current generated-service supply-chain gates.
- The publish contract remains tag-gated for generated services.
- Known advisories and other deferred findings have an explicit maintainer acceptance and follow-up; green CI does not imply that no advisories remain.
- No release or promotion doc contains maintainer-local absolute paths.

## Release Steps

1. Add the dated release section to `CHANGELOG.md` on `dev`, retaining `Unreleased` for future changes, and confirm the preparation commit passes CI.
2. Promote the release candidate from `dev` to `main` through a reviewed pull request.
3. Verify Repository Validation, Starter Validation, and CodeQL on the merged `main` commit.
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
