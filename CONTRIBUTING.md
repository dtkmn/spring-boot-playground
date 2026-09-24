# Contributing

## Purpose

This public repository provides a practical Spring service starter. Contributions
should make generated services easier to adopt and maintain. Optional integrations
belong in `examples/` so the core starter stays focused.

Start with the [open issues](https://github.com/dtkmn/spring-boot-playground/issues)
and their scope and acceptance criteria. Comment on the issue before starting
substantial work to coordinate with other contributors. Use the root README's
generation command from your modified working branch so validation exercises
your changes. Then use the generated README for build, test, and extension commands.

## Rules

- Keep `mvc-jpa` as the default path unless an ADR changes that decision.
- Keep `webflux-r2dbc` supported, but isolated from the default runtime path.
- Put optional integrations in `examples/`.
- Prefer stable and widely adopted libraries over trend-driven additions.
- Update docs and roadmap issues when changing starter behavior.

## Branching And Stabilization

- `dev` is the default integration branch.
- `main` is the stabilized release branch.
- Open pull requests to `dev` unless the change is an approved release or critical hotfix.
- Promote changes from `dev` to `main` only after validation passes and review is complete.
- Create release tags only from commits that are already contained in `main`.
- Do not publish from feature branches, ad hoc commits, or manual workflow dispatches.

## Pull Requests

Every pull request should include:
- a concise problem statement
- the starter surface that changed
- validation performed
- follow-up issues if the change is partial

## Reviews

Reviews should prioritize:
- behavior regressions
- starter complexity growth
- operational risk
- documentation drift
- upgrade and support implications

## Release Changes

- Update `CHANGELOG.md` for user-visible starter changes.
- Follow `RELEASING.md` when promoting `dev` to `main` or cutting a release.
