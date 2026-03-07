# Contributing

## Purpose

This repository is the internal Spring service starter. Changes should improve the default engineering path, not add general-purpose demo code into the core starter contract.

## Rules

- Keep `mvc-jpa` as the default path unless an ADR changes that decision.
- Keep `webflux-r2dbc` supported, but isolated from the default runtime path.
- Put optional integrations in `examples/`.
- Prefer stable and widely adopted libraries over trend-driven additions.
- Update docs and roadmap issues when changing starter behavior.

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
