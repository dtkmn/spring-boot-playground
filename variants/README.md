# Variants

- `mvc-jpa`: default and recommended starter for most services
- `webflux-r2dbc`: supported advanced variant for reactive workloads

Use `./scripts/init-service.sh` to generate a new service from one of these templates.

Each generated service includes:
- application code and tests
- `.dockerignore` and `.gitignore`
- Dockerfile, Docker Compose, and `.env.example`
- starter CI workflow and tag-gated publish workflow
- Dependabot plus baseline support/changelog docs
- `RELEASING.md` with stabilization and release rules
- vendored Helm chart and service-specific values files
