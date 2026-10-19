# Support Policy

This generated AI workflow is experimental, with best-effort starter support for
host development and local Docker Compose. The application maintainer defines
its support lifetime and response commitments.

## Scope

- Java 21, Spring MVC/JPA, PostgreSQL and Flyway.
- Spring AI/Ollama classification with Java-owned authorized read-only lookups.
- Model-free ordinary checks, optional live-model evaluation, and local container checks.
- Test, coverage and CycloneDX reports; a digest-pinned non-root Java runtime.

There is no MCP adapter, Helm chart or supported production deployment. The
two-token identity is a local fixture, not a production authentication system.
Model requests are opt-in, but authentication credentials are required even for
the model-free API. The reference model is `qwen3.5:4b`; other installed models
require evaluation and are not assumed compatible. No model response-time or
general language-accuracy commitment is provided.

## Ownership and updates

Maintain application code, identity, secrets, dependencies, workflows and copied
starter assets. Dependabot can propose dependency updates; it does not synchronize
copied application code or configurations. Review source starter release notes and
the origin in `STARTER.md` when adopting changes.

Prioritize security and critical regressions, validate compatible patches, and
plan broader refreshes quarterly. Run deterministic checks after dependency or
workflow changes, and the separate live evaluations after changing the prompt,
model or provider configuration. Preserve applied Flyway history and record
adopted starter changes. Publishing an application image does not extend this
experimental support scope.
