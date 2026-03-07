# Releasing

## Branch Model

- `dev` is the integration branch.
- `main` is the stabilized release branch.
- Release tags must be created from commits already contained in `main`.

## Version Tags

- Use semantic version tags in the form `v<major>.<minor>.<patch>`.
- Publishing is enabled only for tag pushes that match `v*`.
- Manual publish dispatches are intentionally disabled.

## Release Checklist

1. Ensure `CHANGELOG.md` is updated.
2. Confirm CI is green on `dev`.
3. Promote the intended release commit to `main`.
4. Create and push the version tag from the `main` commit.
5. Verify the publish workflow completed and pushed the image to GHCR.

## Notes

- The publish workflow verifies that the tagged commit is contained in `main`.
- If a hotfix is released from `main`, merge it back into `dev` immediately after the release.
