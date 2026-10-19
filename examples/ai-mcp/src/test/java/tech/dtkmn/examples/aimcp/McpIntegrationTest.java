package tech.dtkmn.examples.aimcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort int port;
    @Autowired ApplicationContext context;

    @Test
    void exposesTheSameReadOnlyCustomerOverRestAndMcpWithoutAnAiProvider() throws Exception {
        assertThat(context.getBeansOfType(ChatModel.class)).isEmpty();
        try (var http = HttpClient.newHttpClient(); var mcp = mcpClient("http://localhost:" + port)) {
            mcp.initialize();
            var tools = mcp.listTools().tools();
            assertThat(tools).extracting(McpSchema.Tool::name).containsExactly("lookup_customer");
            assertThat(tools.getFirst().annotations().readOnlyHint()).isTrue();
            assertThat(tools.getFirst().annotations().destructiveHint()).isFalse();

            var rest = http.send(request("/api/v1/customers/1").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(rest.statusCode()).isEqualTo(200);
            var customer = JSON.readTree(rest.body());
            assertThat(customer).isEqualTo(JSON.readTree("""
                {"id":1,"firstName":"John","lastName":"Doe"}
                """));
            var lookup = payload(mcp.callTool(McpSchema.CallToolRequest.builder("lookup_customer")
                .arguments(Map.of("id", 1)).build()));
            assertThat(lookup.path("found").asBoolean()).isTrue();
            assertThat(lookup.path("customer")).isEqualTo(customer);

            var summary = http.send(request("/api/ai/summary").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"A deployment report\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(summary.statusCode()).isEqualTo(404);
        }
    }

    @Test
    void handlesMissingCustomersAndRejectsInvalidOrDestructiveCalls() throws Exception {
        try (var http = HttpClient.newHttpClient(); var mcp = mcpClient(null)) {
            mcp.initialize();
            assertThat(http.send(request("/api/v1/customers/999").GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
            var missing = payload(mcp.callTool(McpSchema.CallToolRequest.builder("lookup_customer")
                .arguments(Map.of("id", 999)).build()));
            assertThat(missing.path("found").asBoolean()).isFalse();
            assertThat(missing.path("customer").isNull()).isTrue();

            assertThat(http.send(request("/api/v1/customers/0").GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
            var invalid = mcp.callTool(McpSchema.CallToolRequest.builder("lookup_customer")
                .arguments(Map.of("id", 0)).build());
            assertThat(invalid.isError()).isTrue();
            assertThat(text(invalid)).contains("Customer ID must be positive");

            for (var arguments : List.<Map<String, Object>>of(Map.of(), Map.of("id", 1.9),
                    Map.of("id", "1"), Map.of("id", "not-a-number"))) {
                try {
                    var result = mcp.callTool(McpSchema.CallToolRequest.builder("lookup_customer")
                        .arguments(arguments).build());
                    assertThat(result.isError()).as("Invalid arguments %s", arguments).isTrue();
                } catch (McpError error) {
                    assertThat(error.getJsonRpcError().code()).isEqualTo(-32602);
                }
            }

            assertThatExceptionOfType(McpError.class)
                .isThrownBy(() -> mcp.callTool(McpSchema.CallToolRequest.builder("delete_customer")
                    .arguments(Map.of("id", 1)).build()))
                .satisfies(error -> assertThat(error.getJsonRpcError().code()).isEqualTo(-32602));
            var unchanged = payload(mcp.callTool(McpSchema.CallToolRequest.builder("lookup_customer")
                .arguments(Map.of("id", 1)).build()));
            assertThat(unchanged.path("customer").path("firstName").asString()).isEqualTo("John");
        }
    }

    @Test
    void rejectsHostileAndOpaqueOriginsAndDnsRebindingHosts() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            for (var origin : List.of("https://attacker.example", "null")) {
                var response = http.send(request("/mcp").header("Origin", origin).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).as("Origin %s", origin).isEqualTo(403);
            }
        }
        // A raw request permits testing Host without changing global HTTP client settings.
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(("GET /mcp HTTP/1.1\r\nHost: attacker.example:" + port
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            var response = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            assertThat(response.readLine()).startsWith("HTTP/1.1 403 ");
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(5));
    }

    private McpSyncClient mcpClient(String origin) {
        var headers = HttpRequest.newBuilder();
        if (origin != null) {
            headers.header("Origin", origin);
        }
        var transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port)
            .endpoint("/mcp").requestBuilder(headers).openConnectionOnStartup(false).build();
        return McpClient.sync(transport).requestTimeout(Duration.ofSeconds(5))
            .initializationTimeout(Duration.ofSeconds(5)).build();
    }

    private JsonNode payload(McpSchema.CallToolResult result) {
        assertThat(result.isError()).isFalse();
        return JSON.readTree(text(result));
    }

    private String text(McpSchema.CallToolResult result) {
        assertThat(result.content()).singleElement().isInstanceOf(McpSchema.TextContent.class);
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }
}
