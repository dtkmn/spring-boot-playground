# Pilot Playbook

## Purpose

Use this playbook to validate the starter with a real internal service before closing:
- `/Users/0xdant/dev/spring-boot-playground/issues/60`
- `/Users/0xdant/dev/spring-boot-playground/issues/61`

## Entry Criteria

- The service team has agreed to use the starter without bypassing its defaults on day one.
- The team has named an engineering owner and a reviewer.
- The target variant is clear:
  - `mvc-jpa` for the default service path
  - `webflux-r2dbc` only when there is a genuine reactive requirement
- The team can validate local Docker Compose and Kubernetes deployment with Helm.

## Pilot Steps

1. Generate the service with `/Users/0xdant/dev/spring-boot-playground/scripts/init-service.sh`.
2. Commit the generated service to its own repository without structural rewrites.
3. Reach a local green path:
   - `./gradlew test --no-daemon`
   - `docker compose --env-file .env up --build`
   - `./scripts/smoke-test.sh`
4. Deploy the service to the target development cluster with Helm.
5. Record every starter deviation, missing asset, and operational surprise.
6. File a pilot feedback issue using the `Pilot Feedback` template.
7. Convert valid findings into backlog issues before closing the pilot.

## Success Measures

- The service reaches local green without modifying starter internals.
- The team can deploy the service with the vendored Helm chart.
- No blocker forces a fork away from the starter contract.
- All deviations are documented with issue links or explicit acceptance.

## Required Evidence

- Generated service repository link
- Variant used
- Team and engineering owner
- Time to first local green
- Time to first cluster deployment
- Issues filed from pilot findings
- Recommendation:
  - keep as-is
  - keep with follow-up fixes
  - do not recommend yet

## Exit Criteria

- A pilot feedback issue is filed and reviewed.
- Follow-up issues are created for accepted gaps.
- The roadmap issue for the pilot has links to the service repo and feedback issue.
