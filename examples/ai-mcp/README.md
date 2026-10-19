# Spring AI and MCP example

A small, optional example using Java 21, Spring Boot 4.1.1 and Spring AI 2.0.1. It runs independently of the generated templates.

The default application exposes a REST customer lookup and the same lookup as an MCP tool. Both call `CustomerService`, which contains one synthetic customer: John Doe, ID `1`. No database or model is needed.

The optional `ai` profile adds an Ollama summary endpoint. This example covers these two integrations; it has no conversation history, RAG, vector store or agent framework.

## Run the REST and MCP server

Run commands from the repository root with Java 21 available:

```bash
./gradlew -p examples/ai-mcp bootRun
```

The application listens on `127.0.0.1:8087`.

```bash
curl http://127.0.0.1:8087/api/v1/customers/1
curl http://127.0.0.1:8087/actuator/health
```

The customer response is `{"id":1,"firstName":"John","lastName":"Doe"}`. A missing customer returns HTTP 404.

Connect an MCP client using **Streamable HTTP** at `http://127.0.0.1:8087/mcp`. After initialization, list tools and call:

```json
{"name":"lookup_customer","arguments":{"id":1}}
```

The tool returns `found: true` and the customer, or `found: false` for an absent ID. IDs must be positive. The server uses the `STATELESS` protocol: requests do not create persistent MCP sessions. A plain browser GET to `/mcp` is not a tool invocation.

## Add a local Ollama summary call

Start your existing Ollama installation and check its installed models:

```bash
ollama list
```

The default model is `qwen3:latest`. Use that already installed model, or set `OLLAMA_MODEL` to another installed model. The application never downloads a model automatically. For the MCP integration test below, choose a model that supports tool calling.

Stop the default application, then start it with the `ai` profile. Replace `<installed-model>` with a name shown by `ollama list`:

```bash
OLLAMA_MODEL='<installed-model>' ./gradlew -p examples/ai-mcp bootRun --args='--spring.profiles.active=ai'
```

```bash
curl http://127.0.0.1:8087/api/ai/summary \
  -H 'Content-Type: application/json' \
  -d '{"text":"The customer service now supports read-only lookup over REST and MCP."}'
```

The response contains a `summary` string. The endpoint accepts up to 4,000 characters and allows two simultaneous model calls; excess calls return HTTP 429. Model failures return HTTP 503. The profile sets a 256-token output limit, HTTP timeouts and disables retries.

`OLLAMA_BASE_URL` defaults to `http://127.0.0.1:11434`. Keep it local for this example. The summary endpoint sends the supplied text to Ollama and has no tools attached.

## How the pieces connect

- **Summary endpoint:** the application calls Ollama through Spring AI's `ChatClient` to summarize submitted text.
- **MCP server:** an MCP client can discover and call `lookup_customer`. Serving that tool does not require an LLM.
- **LLM using MCP:** a client supplies the tool schema to Ollama, executes the requested call through MCP, and returns the result to the model. Ollama's native tool-calling API alone does not connect to MCP servers.

The test-side [McpChatClient](src/test/java/tech/dtkmn/examples/aimcp/McpChatClient.java) shows the third flow against this server after it has started. It allows only the lookup tool, caps execution at two tool calls, and records tool names, arguments and actual MCP results. The server does not connect back to itself during startup.

## Verify

```bash
./gradlew -p examples/ai-mcp test
```

Regular tests use a fake Ollama HTTP service, so they require no installed model. They cover application behavior without depending on generated LLM wording.

With Ollama running, replace `<installed-tool-capable-model>` with the name of an installed model that supports tool calling, then run the separate opt-in integration check:

```bash
OLLAMA_MODEL='<installed-tool-capable-model>' ./gradlew -p examples/ai-mcp ollamaTest
```

This check makes real local inference requests, including an Ollama-requested MCP customer lookup. It consumes local compute and is separate from the regular test task. Model output can vary; a convincing answer alone is not proof that a tool was called.

## Run with Docker Compose

```bash
docker compose -f examples/ai-mcp/docker-compose.yml up --build
# Stop and remove the example container:
docker compose -f examples/ai-mcp/docker-compose.yml down
```

Compose runs the default REST/MCP application and publishes it only on `127.0.0.1:8087`. Set `APP_PORT` to change the published port. Run the `ai` profile on the host for the simplest access to your existing Ollama service; container loopback refers to the container itself.

## Adapt a generated MVC/JPA service

Import the Spring AI BOM into the service's existing dependency-management block and add the MCP server starter:

```groovy
dependencyManagement {
    imports {
        mavenBom 'org.springframework.ai:spring-ai-bom:2.0.1'
    }
}
dependencies {
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
}
```

Use the MCP settings in [application.yaml](src/main/resources/application.yaml) and adapt [CustomerTools](src/main/java/tech/dtkmn/examples/aimcp/CustomerTools.java) to call your **existing** `CustomerService`. Keep the service's persistence, validation and authorization. The in-memory customer fixture belongs to this example; do not copy it over your real service.

The generated MVC service returns `CustomerResponse` directly and throws HTTP 404 when absent, while this fixture returns `Optional<Customer>`. Adjust the adapter's result type and map only that not-found case to `found: false`; preserve other errors and validate positive IDs before calling the service.

Add `spring-ai-starter-model-ollama` only if the service itself needs model calls, with the relevant settings from [application-ai.yaml](src/main/resources/application-ai.yaml). An MCP server can remain independent of any model provider. MCP clients and their tool loops belong in the consuming application.

Register only the operations you intend to expose. This example exposes one read-only lookup; it does not automatically publish REST endpoints or write operations as tools. The MCP read-only annotation is a hint, so the implementation must enforce that behavior.

## Local boundary and maintenance scope

This is a local demonstration with **no authentication**. Host checks accept `localhost` and `127.0.0.1`; when an Origin header is supplied, it must match one of those hosts and the request port. These checks and loopback binding do not provide user authorization. Do not expose this configuration publicly.

For a real deployment, apply your service's authentication and per-tool, per-tenant authorization to MCP calls. Decide which customer data a caller may receive before invoking the service. Treat prompts and tool results as untrusted input, and review which data reaches the selected model provider. Local models use CPU/GPU memory and time; remote providers may also charge per request or token.

Keep the example's scope to these small integrations and deterministic tests. Recheck Spring AI compatibility and MCP behavior when updating dependencies; local model checks remain optional. See the upstream [MCP server documentation](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) and [Ollama integration](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html).
