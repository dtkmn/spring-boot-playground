# Release Readiness Checklist

## Purpose

Use this checklist before closing `/Users/0xdant/dev/spring-boot-playground/issues/62` and creating `v1.0.0`.

## Entry Criteria

- `/Users/0xdant/dev/spring-boot-playground/issues/60` is closed with pilot evidence.
- `/Users/0xdant/dev/spring-boot-playground/issues/61` is closed with pilot evidence.
- `dev` is green.
- `main` is reserved for stabilized release promotions only.

## Readiness Gates

### Product

- `mvc-jpa` is the documented default path.
- `webflux-r2dbc` is still explicitly marked as the advanced variant.
- Optional integrations remain isolated under `examples/`.

### Quality

- Repository validation passes on `dev`.
- Starter validation passes on `dev`.
- Generated starters still pass build, Docker Compose validation, Helm render, and smoke test.

### Governance

- `/Users/0xdant/dev/spring-boot-playground/CHANGELOG.md` reflects the release scope.
- `/Users/0xdant/dev/spring-boot-playground/SUPPORT.md` and `/Users/0xdant/dev/spring-boot-playground/RELEASING.md` are current.
- The publish contract remains tag-gated for generated services.

## Release Steps

1. Promote the release candidate from `dev` to `main`.
2. Verify validation workflows on `main`.
3. Update `CHANGELOG.md` with a dated `v1.0.0` section.
4. Create and push the annotated tag:

```bash
git checkout main
git pull --ff-only
git tag -a v1.0.0 -m "Spring Service Starter v1.0.0"
git push origin v1.0.0
```

5. Create the GitHub release using the changelog summary.

## Post-Release Backlog

- Open or update a `v1.1` backlog issue.
- Link pilot follow-up issues that did not block `v1.0.0`.
- Separate:
  - must-fix before wider adoption
  - safe for next minor release
  - examples-only improvements

## Exit Criteria

- The `v1.0.0` tag exists on GitHub.
- The GitHub release is published.
- The post-release backlog issue is linked from `/Users/0xdant/dev/spring-boot-playground/issues/62`.
