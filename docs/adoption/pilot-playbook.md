# Pilot Playbook

## Purpose

Use this playbook to validate a freshly generated service locally or during real service adoption. A maintainer-run trial is sufficient for starter validation; a separate team or service repository is optional.

Summarize the results in the existing pilot or release tracker. A `Pilot Feedback` issue can be used when collecting feedback from another team.

## Entry Criteria

- A maintainer or service owner will run the trial and review the results.
- The target variant is clear:
  - `mvc-jpa` for the default service path
  - `webflux-r2dbc` for reactive-variant validation or a service with a reactive requirement
- Docker Compose and a local or development Kubernetes cluster are available.

## Pilot Steps

1. Generate the service from the repository root with `./scripts/init-service.sh`.
2. Follow its generated README and copy `.env.example` to `.env` for local configuration.
3. From the generated service directory, reach a local green path:
   - `./gradlew check --no-daemon`
   - `docker compose -f compose.yaml --env-file .env config`
   - `docker compose -f docker-compose.yml --env-file .env config`
   - `./scripts/dev-smoke-test.sh`
   - `./scripts/smoke-test.sh`
4. Deploy the service with its Helm chart to a local or development cluster.
5. Verify the API, validation errors, health and metrics endpoints, and data persistence after an application restart. For WebFlux, include concurrent requests.
6. Record necessary changes, missing assets, and follow-up findings. Commit the service to its own repository if adopting it for ongoing work.

## Success Measures

- The service reaches local green without modifying starter internals.
- The service deploys with the vendored Helm chart.
- No blocker forces a fork away from the starter contract.
- All deviations are documented with issue links or explicit acceptance.

## Required Evidence

- Variant used
- Starter revision and commands or CI runs used
- Test, smoke-test, deployment, and API verification results
- Confirmation that SBOM and coverage reports were produced
- Findings fixed or accepted for follow-up
- Recommendation:
  - keep as-is
  - keep with follow-up fixes
  - do not recommend yet

Detailed logs may remain private. For a service adoption pilot, also record the service repository and setup difficulties when useful.

## Exit Criteria

- The maintainer or service owner accepts the trial results.
- Blocking findings are fixed; accepted gaps are recorded for follow-up.
- The existing pilot or release tracker contains the result summary.
