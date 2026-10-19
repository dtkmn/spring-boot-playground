# __SERVICE_NAME__

An experimental Java 21 MVC/JPA order-support service with PostgreSQL, Flyway and
optional Spring AI/Ollama classification. It supports host development and local
Docker Compose. It does not include a Helm chart or production authentication.

The copied scaffold uses the [MIT License](LICENSE). Preserve its copyright and
permission notice; third-party components retain their own licenses. See
[STARTER.md](STARTER.md) for the generated origin and selected capabilities.

## First run

Use a Java 21 JDK, Docker with Compose v2, Bash and curl. The Gradle wrapper
downloads Gradle when needed; builds also need dependencies and container images.
OpenSSL is needed only for the credential-generation command below.

The credential fields in `.env.example` are intentionally empty. In a fresh
generated service, create private values:

```bash
umask 077
{
  printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 24)"
  printf 'PILOT_CALLER_A_TOKEN=%s\n' "$(openssl rand -hex 24)"
  printf 'PILOT_CALLER_B_TOKEN=%s\n' "$(openssl rand -hex 24)"
  printf 'PILOT_PROFILES=docker\n'
} > .env
chmod 600 .env
set -a
source .env
set +a
./gradlew bootRun
```

Keep `.env` local and out of Git. Retain those values when restarting; this setup
command overwrites the file. The two tokens must differ and contain at least 32
non-whitespace characters. They are required even when the model is disabled.
Only Authorization-header Bearer credentials are accepted. This local fixture
maps them to `caller-a` and `caller-b`; replace it with appropriate identity and
authorization before using real business data or deploying publicly.

Boot starts PostgreSQL using `compose.yaml`. Flyway creates these synthetic
records on a fresh database:

| Caller | Order | Order status | Shipment |
| --- | --- | --- | --- |
| caller-a | 1001 | DELIVERED | DELIVERED |
| caller-a | 1002 | SHIPPED | DELAYED; Carrier backlog; no estimated date |
| caller-b | 2001 | SHIPPED | A separate shipment fixture |

The default application binds to `127.0.0.1:__APP_PORT__`, management to
`127.0.0.1:__MANAGEMENT_PORT__`. Load `.env` in a second terminal with the same
`set -a` / `source .env` / `set +a` commands. This helper supplies the credential
through curl configuration stdin:

```bash
authcurl() {
  local token=$1
  shift
  printf 'header = "Authorization: Bearer %s"\n' "$token" | curl --config - "$@"
}
authcurl "$PILOT_CALLER_A_TOKEN" -fsS http://127.0.0.1:__APP_PORT__/api/v1/orders/1001
curl -fsS http://127.0.0.1:__MANAGEMENT_PORT__/actuator/health
```

The Order API returns `{"id":1001,"status":"DELIVERED"}`. Missing orders and
another caller's orders both return 404. Health/probes expose status without
authentication; metrics, info and business routes require authentication.
The default configuration uses no model; the AI endpoint returns 503 until the AI
profile is enabled. Press Ctrl-C to stop the host application
and its managed database; keep the database container to retain development data.

For host port conflicts, set `SERVER_PORT` and `MANAGEMENT_SERVER_PORT` before
`bootRun`. `APP_HOST_PORT`, `MANAGEMENT_HOST_PORT` and `POSTGRES_EXPORT_PORT` in
`.env` control Compose exports; the database export defaults to `5432`.

## Enable the AI profile

Start your existing Ollama server and inspect installed models with `ollama list`.
The reference model is `qwen3.5:4b`; select an already installed model that supports
Ollama JSON-schema output. Other models need their own evaluation. The application
never downloads a model automatically.

Stop the default application, then start the model-enabled profile from the
terminal where `.env` is loaded. Replace `<installed-model>` with its actual name:

```bash
OLLAMA_MODEL='<installed-model>' OLLAMA_BASE_URL=http://127.0.0.1:11434 \
  SPRING_PROFILES_ACTIVE=ai ./gradlew bootRun
```

Using the authenticated helper above:

```bash
authcurl "$PILOT_CALLER_A_TOKEN" -fsS \
  -H 'Content-Type: application/json' \
  -d '{"orderId":1002,"question":"Why is this order delayed?"}' \
  http://127.0.0.1:__APP_PORT__/api/ai/order-support
```

The model makes one intent-only classification: `STATUS`, `DELAY_REASON` or
`CLARIFY`. It receives the question, with no application records, authenticated
identity or tools attached. Java chooses the read-only lookup sequence, enforces
the caller and selected order, and constructs the answer, typed facts and evidence
from retrieved records. This is a bounded business workflow, not model-directed
tool calling or a general agent.

For order 1002, a delay answer reports `SHIPPED`, shipment `DELAYED`, the recorded
reason `Carrier backlog`, and no estimated delivery date. Status and delay-reason
questions are supported; dates, prices, history, writes and vague requests return
clarification. No order ID also returns clarification without model inference.
Requests accept a positive integer `orderId` and a nonblank question up to 1,000
characters. Unexpected fields are rejected.

Each invocation allows at most one model call, two Java business lookups, 256
output tokens and a 60-second deadline. Two occupied invocations are allowed;
excess requests return 429. HTTP connect/read limits are 3/20 seconds and database
operations have short five-second bounds. There are no retries. A canceled worker
keeps its slot until it exits; an HTTP timeout does not prove Ollama computation
stopped. Contract/provider/database failures return generic 503, expiry returns
504, invalid requests return 400 and invalid credentials return 401.
CSRF protection remains enabled; Spring Security's resource-server configuration
exempts Bearer requests, so clients do not need a separate CSRF token.

## Checks and local containers

With Docker running, ordinary checks use a fake Ollama HTTP endpoint and disposable
PostgreSQL; no installed model is required:

```bash
./gradlew check bootJar
```

For real local inference, select an installed model explicitly:

```bash
OLLAMA_MODEL='<installed-model>' ./gradlew ollamaTest
OLLAMA_MODEL='<installed-model>' ./gradlew ollamaExploreTest
OLLAMA_MODEL='<installed-model>' ./gradlew ollamaHeldoutTest
```

These separate tasks cover 15 original sample trials, 32 broader trials including
eight boundary cases, and 36 fresh-wording trials. They consume local compute and
do not run in ordinary CI. They check model decisions, actual Java lookups and
facts; passing these samples does not establish accuracy for arbitrary language.

Start the model-free packaged application and PostgreSQL:

```bash
docker compose -f docker-compose.yml --env-file .env up --build
```

Opt in to classification in the container instead:

```bash
PILOT_PROFILES=docker,ai OLLAMA_MODEL='<installed-model>' \
  docker compose -f docker-compose.yml --env-file .env up --build
```

Compose publishes app, management and database ports only on loopback. The app
binds to `0.0.0.0` inside its container and reaches host Ollama through
`http://host.docker.internal:11434`; Ollama must be reachable at that address.
Container loopback refers to the container, not the host model server.
Use `docker compose -f docker-compose.yml --env-file .env stop` to stop containers
while retaining the development database for a later `up`. The database uses an
anonymous volume; `down` removes its container, and a later `up` does not
automatically reuse that data. Use `down -v` only to discard disposable data.

The existing `scripts/dev-smoke-test.sh` and `scripts/smoke-test.sh` exercise the
model-free host/container API, caller isolation, health and authenticated metrics.
They remove disposable database volumes. Run them from a separate generated copy
when keeping development data, with the normal application stopped.

## Adapt and maintain

Replace the sample Order/Shipment domain, seed data, classification contract,
canonical formatting and local identity fixture with your application's rules.
Both REST and AI paths should keep using the same authorized business services.
Never replace already-applied Flyway migrations with a fresh sample schema.

Metrics describe model calls, Java lookup attempts, tokens and latency without
caller/order/question labels. Prompt/result content and tokens do not belong in
routine logs. Actuator/Prometheus and optional structured logging/tracing remain
available; configure collectors and sampling through environment settings.
`./gradlew check` produces test, JaCoCo and CycloneDX reports under `build/reports/`.

This experimental output has no MCP adapter or Helm deployment. See [SUPPORT.md](SUPPORT.md)
for its scope and [RELEASING.md](RELEASING.md) for the application release workflow.
Follow the [manual upgrade guide](https://github.com/dtkmn/spring-boot-playground/blob/main/docs/adoption/upgrading-generated-services.md)
when adopting newer starter assets; copied services receive no automatic source
updates. Preserve the original [STARTER.md](STARTER.md) and record adopted changes.
