# Releasing

## Scope

This document defines how the starter repository is stabilized and released.

## Branch Model

- `dev` is the default integration branch.
- `main` is the stabilized branch used for release tags.
- Normal feature work lands on `dev`.
- `main` only receives reviewed promotions from `dev` or approved critical fixes.

## Versioning

- Use semantic version tags in the form `v<major>.<minor>.<patch>`.
- Create release tags from `main` only.
- Update `/Users/0xdant/dev/spring-boot-playground/CHANGELOG.md` before tagging.

## Release Checklist

1. Confirm the planned release scope is complete.
2. Ensure `dev` is green and documentation is current.
3. Update `/Users/0xdant/dev/spring-boot-playground/CHANGELOG.md`:
   - move finished work out of `Unreleased`
   - add a dated release heading
   - keep entries concise and user-visible
4. Open and merge a reviewed pull request from `dev` to `main`.
5. Verify `main` is green after merge.
6. Create an annotated tag on the `main` commit using `v<major>.<minor>.<patch>`.
7. Push the tag and create GitHub release notes from the changelog summary.
8. Start a fresh `Unreleased` section for follow-up work.

## Publish Policy

- The starter repository validates releases but does not publish a runtime image.
- Generated services publish images only from version tags.
- Generated service publish workflows must not allow manual dispatch-based publishing.
- Generated service tags must reference commits contained in `main`.

## Hotfixes

1. Make the smallest viable fix from `main`.
2. Validate the fix.
3. Tag a patch release from `main`.
4. Merge the hotfix back into `dev` so the branches do not drift.
