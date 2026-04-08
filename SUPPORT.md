# Support Policy

## Audience

This starter is optimized for internal engineering teams.

## Update Cadence

- Quarterly stable dependency and framework updates
- Out-of-band patches only for security issues or critical regressions

## Release Channel

- `dev`: active integration branch
- `main`: stabilized release branch
- `v*` tags: immutable release points

## Compatibility Baseline

- Java 21
- Spring Boot 3.5.x maintained baseline line
- Spring Boot 4.x migration planned as the next modernization tranche
- PostgreSQL as the default persistence contract
- Kubernetes plus Helm as the supported cloud deployment path

## Support Levels

- `mvc-jpa`: primary supported path
- `webflux-r2dbc`: supported advanced variant
- `examples/`: reference only, best-effort support
