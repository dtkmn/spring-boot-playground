# Variants

- `mvc-jpa`: default and recommended starter for most services
- `webflux-r2dbc`: supported advanced variant for reactive workloads

Use `./scripts/init-service.sh` to generate a new service from one of these templates.

Each generated service includes:
- application code and tests
- `.gitignore`
- Dockerfile, Docker Compose, and `.env.example`
- starter CI workflow
- vendored Helm chart and service-specific values files
