package tech.dtkmn.examples.aimcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("ai")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.http.clients.read-timeout=2s")
class AiIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<JsonNode> REQUESTS = new CopyOnWriteArrayList<>();
    private static final java.util.concurrent.ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final HttpServer OLLAMA = startOllama();
    private static volatile String mode = "summary";
    private static volatile CountDownLatch modelEntered;
    private static volatile CountDownLatch releaseModel;

    @LocalServerPort int port;
    @Autowired ChatModel model;

    @DynamicPropertySource
    static void modelEndpoint(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", () -> "http://127.0.0.1:" + OLLAMA.getAddress().getPort());
    }

    @BeforeEach
    void reset() {
        mode = "summary";
        REQUESTS.clear();
    }

    @AfterAll
    static void stopOllama() {
        OLLAMA.stop(0);
        EXECUTOR.shutdownNow();
    }

    @Test
    void sendsBoundedSummaryRequestWithoutTools() throws Exception {
        var response = summarize("The deployment succeeded. All health checks passed.");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).path("summary").asString()).isEqualTo("The deployment is healthy.");
        assertThat(REQUESTS).hasSize(1);
        var request = REQUESTS.getFirst();
        assertThat(request.path("messages").get(1).path("content").asString()).contains("All health checks passed");
        assertThat(request.path("tools").isMissingNode() || request.path("tools").isEmpty()).isTrue();
        assertThat(request.path("stream").asBoolean()).isFalse();
        assertThat(request.path("think").asBoolean(true)).isFalse();
        assertThat(request.path("options").path("num_predict").asInt()).isEqualTo(256);
    }

    @Test
    void rejectsInvalidInputBeforeCallingTheModel() throws Exception {
        assertThat(summarize(" ").statusCode()).isEqualTo(400);
        assertThat(summarize("x".repeat(4001)).statusCode()).isEqualTo(400);
        assertThat(REQUESTS).isEmpty();
    }

    @Test
    void modelFailureDoesNotBreakTheCustomerApiAndCanRecover() throws Exception {
        mode = "failure";
        assertThat(summarize("A deployment report").statusCode()).isEqualTo(503);
        assertThat(REQUESTS).hasSize(1); // No hidden retry storm.
        try (var http = HttpClient.newHttpClient()) {
            assertThat(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/customers/1"))
                .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        }
        mode = "summary";
        assertThat(summarize("The deployment succeeded").statusCode()).isEqualTo(200);
    }

    @Test
    void modelReadTimeoutReturnsServiceUnavailable() throws Exception {
        mode = "slow";
        assertThat(summarize("A deployment report").statusCode()).isEqualTo(503);
        assertThat(REQUESTS).hasSize(1);
        mode = "summary";
        assertThat(summarize("The model recovered").statusCode()).isEqualTo(200);
    }

    @Test
    void rejectsExcessConcurrentSummariesAndRestoresCapacityAfterModelFailures() throws Exception {
        assertHeldSummaries("held-failure", 503);
        assertHeldSummaries("held", 200);
        mode = "summary";
        assertThat(summarize("The model recovered").statusCode()).isEqualTo(200);
    }

    @Test
    void modelUsesAnActualDiscoveredMcpToolAndReceivesItsResult() {
        mode = "lookup";
        try (var client = new McpChatClient(port, model)) {
            var response = client.ask("Look up customer 1");
            assertThat(client.toolResults()).hasSize(1);
            assertThat(client.toolResults().getFirst()).contains("John", "Doe");
            assertThat(response.getResult().getOutput().getText()).contains("John Doe");
            assertThat(REQUESTS).hasSize(2);
            assertThat(REQUESTS.getFirst().path("tools")).hasSize(1);
            assertThat(REQUESTS.getLast().path("messages").toString()).contains("John", "Doe", "tool");
        }
    }

    @Test
    void repeatedModelToolRequestsStopAtTheConfiguredBudget() {
        mode = "loop";
        try (var client = new McpChatClient(port, model)) {
            var response = client.ask("Look up customer 1");
            assertThat(client.toolResults()).hasSize(2);
            assertThat(REQUESTS).hasSize(3);
            assertThat(response.getResult().getMetadata().getFinishReason())
                .isEqualTo(ToolCallLimitExceededException.FINISH_REASON);
        }
    }

    private HttpResponse<String> summarize(String text) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ai/summary"))
                .header("Content-Type", "application/json").timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("text", text))))
                .build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private void assertHeldSummaries(String heldMode, int expectedStatus) throws Exception {
        REQUESTS.clear();
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        modelEntered = entered;
        releaseModel = release;
        mode = heldMode;
        try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = callers.submit(() -> summarize("First deployment report"));
            var second = callers.submit(() -> summarize("Second deployment report"));
            try {
                assertThat(entered.await(2, TimeUnit.SECONDS)).as("Two model calls must be active").isTrue();
                assertThat(summarize("Third deployment report").statusCode()).isEqualTo(429);
                assertThat(REQUESTS).hasSize(2); // Rejected calls never reach the model.
            } finally {
                release.countDown();
            }
            assertThat(first.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(expectedStatus);
            assertThat(second.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(expectedStatus);
        }
    }

    private static HttpServer startOllama() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(EXECUTOR);
            server.createContext("/api/chat", AiIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            var request = JSON.readTree(exchange.getRequestBody());
            REQUESTS.add(request);
            String currentMode = mode;
            if (currentMode.startsWith("held")) {
                var entered = modelEntered;
                var release = releaseModel;
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        exchange.sendResponseHeaders(503, -1);
                        return;
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (currentMode.equals("slow")) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (currentMode.equals("failure") || currentMode.equals("held-failure")) {
                exchange.sendResponseHeaders(503, -1);
                return;
            }
            var messages = request.path("messages");
            boolean hasToolResult = messages.toString().contains("\"role\":\"tool\"");
            Object message = Map.of("role", "assistant", "content", "The deployment is healthy.");
            if (currentMode.equals("loop") || (currentMode.equals("lookup") && !hasToolResult)) {
                String tool = request.path("tools").get(0).path("function").path("name").asString();
                message = Map.of("role", "assistant", "content", "", "tool_calls", List.of(
                    Map.of("function", Map.of("name", tool, "arguments", Map.of("id", 1)))));
            } else if (currentMode.equals("lookup")) {
                message = Map.of("role", "assistant", "content", "Customer 1 is John Doe.");
            }
            byte[] body = JSON.writeValueAsString(Map.of("model", "qwen3:latest", "message", message,
                "done", true, "done_reason", "stop", "created_at", "2026-09-25T00:00:00Z"))
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
