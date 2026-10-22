# Java Seedworks

Java backend service templates with Spring Boot, PostgreSQL, tests, containers
and Helm. MVC/JPA is the default; WebFlux/R2DBC supports reactive workloads.

Previously `dtkmn/spring-boot-playground`. Update existing clones with:

```bash
git remote set-url origin https://github.com/dtkmn/java-seedworks.git
```

Your local directory can keep its existing name. For installed Codex plugins,
follow the [source migration instructions](#codex-plugin) below.

## Release Scope

This repository provides a reusable Spring service starter. The v1.0 API scope covers service generation, local development, container packaging, and Kubernetes deployment.

Both variants have been validated as generated services with PostgreSQL, Docker Compose, and local Kubernetes. See [adoption guidance](docs/adoption/promotion-brief.md) and the [version policy](docs/releases/version-policy.md). The current baseline is Java 21 plus Spring Boot 4.1.1.

The default path is `mvc-jpa` with `--purpose api`. `webflux-r2dbc` remains the supported advanced API variant for teams with a real reactive requirement. An experimental MVC/JPA AI purpose adds an order-support workflow for host and local Docker Compose use. Optional integrations under `examples/` remain outside the starter contract.

## Starter Contract

Maintained API outputs provide:
- Java 21 and the maintained Spring Boot 4.1.x line
- PostgreSQL plus Flyway
- Actuator, Prometheus metrics, and OTLP-ready tracing hooks
- Docker Compose-backed local development plus standalone container smoke coverage
- digest-pinned, non-root distroless Java runtime
- test baselines suitable for CI
- Kubernetes deployment support through Helm

The experimental AI output retains the Java, database, observability, container and test baseline. It uses a local authentication fixture and does not include Helm or production deployment support.

The default starter contract does not include:
- Kafka and Kafka Streams
- websocket ingest pipelines
- domain-specific integrations
- a broad demo runtime at the repository root

## Generation Choices

Choose `--purpose api|ai`; the default is `api`. The initial AI integration uses
Ollama, with the installed model and endpoint configured at runtime. MCP is not
included in generated services.

| Variant | Purpose | Support | Deployment |
| --- | --- | --- | --- |
| `mvc-jpa` | `api` | Primary maintained API | Host, Compose, Helm |
| `webflux-r2dbc` | `api` | Maintained advanced API | Host, Compose, Helm |
| `mvc-jpa` | `ai` | Experimental order-support workflow | Host and local Compose |
| `webflux-r2dbc` | `ai` | Unsupported; rejected before generation | — |

API output omits Spring AI, MCP and AI credential settings. AI output includes
an opt-in model profile; its default Order API runs without a model but requires
fresh local authentication tokens. This fixture is not production identity.

## Repository Layout

- `variants/mvc-jpa`: default API starter plus experimental AI assets
- `variants/webflux-r2dbc`: supported advanced variant for reactive workloads
- `examples/kafka-basic`: minimal HTTP-to-Kafka example
- `examples/kafka-streams`: isolated stream-processing example
- `examples/binance-websocket`: websocket-to-Kafka market-data example
- `examples/ai-mcp`: REST and read-only MCP example with optional Ollama summaries
- `deploy/helm/spring-service-starter`: shared Kubernetes deployment chart
- `docs/adr`: architecture decision records
- `docs/adoption`: pilot execution guidance
- `docs/releases`: release readiness checklists
- `scripts/init-service.sh`: starter bootstrap script

## Start A New Service

Prerequisites: Git, a Java 21 JDK, a running Docker engine with Docker Compose v2,
and Bash with standard Unix tools (`tar`, `find`, and `curl`). Use macOS, Linux,
or WSL2.
The generated Gradle wrapper downloads Gradle; no separate Gradle installation is
needed. The first run also downloads dependencies and container images.

The traditional clone-and-script workflow below remains supported. The
[Codex](#codex-plugin) and [Claude Code](#claude-code-plugin) plugins provide
additional generation entry points using the same assets.

Start from the stabilized `main` branch in a new directory:

```bash
git clone --branch main --depth 1 https://github.com/dtkmn/java-seedworks.git
cd java-seedworks
./scripts/init-service.sh \
  --variant mvc-jpa \
  --service-name customer-profile \
  --group-id tech.company.platform \
  --artifact-id customer-profile \
  --package-name tech.company.platform.customerprofile
cd generated/customer-profile
cp .env.example .env
./gradlew bootRun
```

Keep that terminal running. Spring Boot starts PostgreSQL through `compose.yaml`
and Flyway creates the schema and seed data. Once the application has started,
run this request in a second terminal:

```bash
curl -fsS http://localhost:8080/api/v1/customers
```

A fresh database returns:

```json
[{"id":1,"firstName":"John","lastName":"Doe"}]
```

Press Ctrl-C in the first terminal to stop the application and its managed database.
Restart with `./gradlew bootRun` from `generated/customer-profile`; keep the database
container to retain local data. For build/test commands, port overrides, and where
to extend the application, see the current
[MVC/JPA instructions](variants/mvc-jpa/template/README.md) or
[WebFlux/R2DBC instructions](variants/webflux-r2dbc/template/README.md).

For the reactive variant, choose `--variant webflux-r2dbc` and an unused artifact
name/output directory. The generation and first-run steps are otherwise the same.
The `dev-smoke-test.sh` and `smoke-test.sh` scripts are optional, disposable checks:
they remove containers and database volumes before and after running. Use a
separate generated copy for smoke checks when you want to keep development data.

Output is created under `generated/<artifact-id>` by default.

For the experimental AI workflow, generate a separate service:

```bash
./scripts/init-service.sh \
  --variant mvc-jpa \
  --purpose ai \
  --service-name order-support \
  --group-id tech.company.platform \
  --artifact-id order-support \
  --package-name tech.company.platform.ordersupport
```

Follow the [AI first-run instructions](variants/mvc-jpa/ai/README.md) to create
private credentials and start the Order API, then opt in to an installed Ollama
model. The model classifies a question as status, delay reason or clarification.
Java performs owner-scoped read-only lookups and constructs the factual answer;
the model receives no application records or authenticated identity and has no
tools attached. The sample domain and local identity fixture are replaceable
application code, not a general agent platform.

The raw templates use the repository's shared Gradle wrapper. The generator
copies that wrapper into each generated service; do not add separate wrappers
under `variants/*/template`. In IntelliJ IDEA, import a generated service as a
Gradle project. Linking a raw template can cause the IDE to generate another
wrapper inside it.

To pin a release, replace `--branch main` with a tag such as `--branch v1.0.0`.
That older release still requires Perl for generation and predates bundled license
files; copy [LICENSE](LICENSE) into services generated from it to retain the
starter's copyright and permission notice. Generation from this checkout includes
the assets below.

Generated services include:
- application code for the selected variant
- the starter's MIT license and copyright notice
- `STARTER.md` with the source commit, release status, variant, purpose, model integration, generator contract, and original generation options
- `.dockerignore` and `.gitignore`
- `gradlew`, `gradlew.bat`, and `gradle/wrapper`
- a starter CI workflow under `.github/workflows/ci.yml`
- a tag-gated publish workflow under `.github/workflows/publish.yml`
- Dependabot and baseline support/changelog docs
- CycloneDX SBOM generation through `./gradlew check`
- JaCoCo coverage reports from the default test lifecycle
- `RELEASING.md` with branch and tag rules
- `compose.yaml` for `bootRun` development services
- `docker-compose.yml` for full container smoke validation
- `scripts/dev-smoke-test.sh` and `scripts/smoke-test.sh`
- local env template plus opt-in structured logging and OTLP tracing hooks
- digest-pinned distroless Java runtime running as non-root
- for API output, a vendored Helm chart under `deploy/helm/spring-service-starter` and service-specific values for dev, staging, and prod

For an existing service, follow the [manual upgrade guide](docs/adoption/upgrading-generated-services.md)
to compare starter versions and apply relevant changes while preserving application code.

## Codex plugin

The Java Seedworks plugin is an experimental plugin for Codex. It can
generate the same three outputs listed above and help verify them. API support
and the experimental AI scope remain as documented; plugin use does not add AI
dependencies to ordinary APIs.

Install directly from GitHub without cloning the repository. With a Codex CLI
providing `plugin add` (validated with 0.163.0):

```bash
codex plugin marketplace add dtkmn/java-seedworks --ref main
codex plugin add java-seedworks@java-seedworks
codex plugin list --marketplace java-seedworks
```

For the Codex desktop app, register the GitHub marketplace with the first command,
quit and reopen the app, then install Java Seedworks from that source in
**Plugins**. If you already installed it with the CLI, check that it is enabled
in the app. Start a new local conversation after installation.

In a new local Codex conversation, ask:

> Use Java Seedworks to generate a service named orders-api in my projects
> directory, using PostgreSQL, tests and Docker. Ask me for any missing details.

The `generate-service` skill resolves the installed assets, writes to your chosen
new project directory, and runs the generated Gradle checks when Java 21 and
Docker are ready. Inspect `STARTER.md` for the generation baseline. Java/Docker
are not needed just to copy the scaffold; they are needed to build and run it.
Use the generated README for first-run credentials and optional AI setup.

Maintainers can test changes before pushing by preparing a local package. Choose
a new directory outside the checkout whose parent already exists:

```bash
./scripts/package-plugin.sh /tmp/java-seedworks-plugin
codex plugin marketplace add /tmp/java-seedworks-plugin
codex plugin add java-seedworks@java-seedworks
```

The package contains the generator, templates, wrapper, license, API deployment
assets, generation skill and marketplace catalog. It excludes local build/cache
files and private environment files. `STARTER_SOURCE` retains the source commit,
release label when available, and clean/modified state. A package made from a
modified checkout retains that fact; its plugin version is not a claim that the
starter has been released.

If your marketplace uses the former `dtkmn/spring-boot-playground` source,
remove the previous installation and marketplace first. Use the same steps to
reinstall from updated GitHub contents, switch between GitHub and a local
package, or change the local package path:

```bash
codex plugin remove java-seedworks@java-seedworks
codex plugin marketplace remove java-seedworks
```

Then repeat the installation commands for your chosen source and start a new
conversation. Maintainers should bump `plugin.json`'s version when publishing
changed plugin assets. An installed plugin uses its cached copy; reinstalling
does not update existing generated applications. Those applications retain the
manual update process in the
[upgrade guide](docs/adoption/upgrading-generated-services.md).

GitHub installation does not require a release archive or an official plugin
directory listing. The manual generator remains available without installing
Codex.

## Claude Code plugin

The experimental Claude Code adapter uses the same `generate-service` skill,
generator and assets as the Codex plugin. It supports the same three generation
choices and support boundaries above. Ordinary API output adds no runtime AI.
Generation needs Bash and standard Unix tools; Java 21 and Docker are needed
for the generated checks and application.

With Claude Code installed, register the GitHub marketplace and install:

```bash
claude plugin marketplace add 'dtkmn/java-seedworks#main'
claude plugin install java-seedworks@java-seedworks
claude plugin list
```

Start a new Claude Code session in your workspace, then invoke the skill:

```text
/java-seedworks:generate-service Create a customer-profile MVC/JPA API in my projects directory with PostgreSQL, tests and Docker. Ask me for any missing details.
```

Choose a new destination outside the plugin installation. The skill gathers
missing coordinates and runs `./gradlew check bootJar` when prerequisites are
ready. The API begins with the Customer sample domain; follow the generated
README to run or adapt it and inspect `STARTER.md` for its source baseline.

For local testing before pushing, prepare a new package with
`./scripts/package-plugin.sh /tmp/java-seedworks-plugin` as described above,
then register that directory instead of the GitHub source:

```bash
claude plugin marketplace add /tmp/java-seedworks-plugin
claude plugin install java-seedworks@java-seedworks
```

For updates, refresh the marketplace and installed plugin, then start a new
session:

```bash
claude plugin marketplace update java-seedworks
claude plugin update java-seedworks@java-seedworks
```

An update changes the installed starter, not services already generated from it.
Those services follow the [manual upgrade guide](docs/adoption/upgrading-generated-services.md).

## Variants

### `mvc-jpa`
Use this when:
- the service is request/response heavy
- the team wants the lowest-friction Spring path
- JPA and standard blocking I/O are acceptable

### `webflux-r2dbc`
Use this when:
- the service has a real reactive requirement
- the team is comfortable with Reactor and reactive persistence
- non-blocking I/O provides clear value

## Examples

Examples are runnable, isolated projects. They exist to show how to add optional integrations without forcing those choices into every new service.

Run an example directly from its directory, for example:

```bash
./gradlew -p examples/kafka-basic test
cp examples/kafka-basic/.env.example examples/kafka-basic/.env
cd examples/kafka-basic && docker compose --env-file .env up --build
```

## Kubernetes And Helm

The supported Kubernetes deployment path for maintained API output lives in
`deploy/helm/spring-service-starter`. Experimental AI output does not copy this
chart or advertise Kubernetes deployment support.

Supported chart contract:
- `image.repository`
- `image.tag`
- `service.port`
- `env`
- `secrets.existingSecret`
- `serviceAccount`
- `podAnnotations`
- `podSecurityContext`
- `containerSecurityContext`
- `resources`
- `autoscaling.enabled`
- `ingress.enabled`
- `ingress.hosts`
- `nodeSelector`
- `tolerations`
- `affinity`
- `podDisruptionBudget`
- `postgres.enabled`
- `postgres.host`
- `postgres.port`
- `postgres.database`
- `management.port`

Default deployment assumptions:
- external PostgreSQL
- no bundled Kafka
- Actuator-backed health probes
- HPA support is optional and disabled by default
- Java 25 migration remains a separate modernization tranche

## Governance

- `SUPPORT.md`: support window and update cadence
- `CHANGELOG.md`: release history
- `RELEASING.md`: release process and stabilization rules
- `CONTRIBUTING.md`: contribution and review rules
- `docs/adoption/pilot-playbook.md`: pilot execution and evidence rules
- `docs/adoption/promotion-brief.md`: adoption guidance and validation boundaries
- `docs/releases/release-readiness-checklist.md`: `v1.0.0` readiness gates
- `docs/releases/version-policy.md`: Java, Spring Boot, Gradle, and migration policy
- `docs/security/supply-chain-baseline.md`: SBOM, Dependabot, and scanner guardrails
- `docs/adr`: architectural decisions

## Root Repository Behavior

The repository root is no longer a runnable Spring application. Runtime code lives under `variants/` and `examples/`. This keeps the root of the repository focused on starter assets, governance, and validation.

## License

The starter's original code, documentation, and templates are available under the
[MIT License](LICENSE). The generator in this checkout includes a copy of this
license for the copied scaffold. You may use and modify it in public, private, or
commercial applications; retain the copyright and permission notice when distributing copies
or substantial portions. Third-party components, including the Gradle wrapper,
retain their own licenses and notices.
