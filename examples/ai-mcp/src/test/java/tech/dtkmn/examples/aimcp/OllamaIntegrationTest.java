package tech.dtkmn.examples.aimcp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ollama")
@ActiveProfiles("ai")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(180)
class OllamaIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort int port;
    @Autowired ChatModel model;

    @Test
    void summarizesThroughSpringAndUsesRealMcpResultsWithTheLocalModel() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ai/summary"))
                .timeout(Duration.ofSeconds(70)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"text":"The service was deployed successfully. All health checks passed."}
                    """)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(response.body()).path("summary").asString()).isNotBlank();
            System.out.println("Local Ollama summary: " + response.body());
        }
        try (var client = new McpChatClient(port, model)) {
            var response = client.ask("Use the lookup tool to find customer 1 and tell me their full name.");
            assertThat(response.getResult().getMetadata().getFinishReason())
                .isNotEqualTo(ToolCallLimitExceededException.FINISH_REASON);
            assertLookup(client, 1, new CustomerService.Customer(1, "John", "Doe"));
            assertThat(response.getResult().getOutput().getText()).contains("John", "Doe");
            System.out.println("Actual MCP result: " + client.toolResults());
            System.out.println("Local Ollama answer: " + response.getResult().getOutput().getText());
        }
        try (var client = new McpChatClient(port, model)) {
            var response = client.ask("Use the lookup tool to check whether customer 999 exists.");
            assertThat(response.getResult().getMetadata().getFinishReason())
                .isNotEqualTo(ToolCallLimitExceededException.FINISH_REASON);
            assertLookup(client, 999, null);
            assertThat(response.getResult().getOutput().getText().toLowerCase()).containsAnyOf("not found", "does not exist", "no customer");
            System.out.println("Missing customer MCP result: " + client.toolResults());
            System.out.println("Local Ollama answer: " + response.getResult().getOutput().getText());
        }
    }

    private void assertLookup(McpChatClient client, long expectedId, CustomerService.Customer expectedCustomer) {
        assertThat(client.toolInvocations()).isNotEmpty();
        for (var invocation : client.toolInvocations()) {
            assertThat(invocation.toolName()).isEqualTo("lookup_customer");
            var arguments = JSON.readTree(invocation.arguments());
            assertThat(arguments.path("id").isIntegralNumber()).isTrue();
            assertThat(arguments.path("id").asLong()).isEqualTo(expectedId);
            var content = JSON.readTree(invocation.result());
            assertThat(content).hasSize(1);
            // The callback's normalized content JSON omits the MCP wire type discriminator.
            assertThat(content.get(0).path("text").asString()).isNotBlank();
            var lookup = JSON.readValue(content.get(0).path("text").asString(), CustomerTools.LookupResult.class);
            assertThat(lookup).isEqualTo(new CustomerTools.LookupResult(expectedCustomer != null, expectedCustomer));
        }
    }
}
