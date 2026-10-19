# Releasing

The application maintainer chooses its release cadence. This generated scaffold
is experimental; its local identity fixture and Compose checks do not establish
production deployment support.

## Branch and tag rules

- `dev` is the integration branch and `main` the stabilized branch.
- Create semantic version tags `v<major>.<minor>.<patch>` from commits contained in `main`.
- The generated publish workflow runs only on `v*` tag pushes; manual publication is disabled.

## Checklist

1. Update `CHANGELOG.md` and review application changes, copied starter assets and dependencies.
2. Run `./gradlew check bootJar` and the disposable host/container smoke checks with fresh local credentials.
3. Run the separate live-model evaluations for changes to classification or model configuration; record the tested model and limitations in the application's release notes.
4. Confirm the application CI is green, then promote the intended commit to `main`.
5. Create and push its version tag, then verify the workflow published the image to GHCR.

The workflow checks that the tagged commit is on `main`, runs deterministic
quality checks and model-free local smoke checks, and publishes a container image.
It does not evaluate live models or deploy the application. Configure the target
environment's identity, secrets and runtime before any deployment. Do not bake
tokens or model credentials into images or commit `.env`.
