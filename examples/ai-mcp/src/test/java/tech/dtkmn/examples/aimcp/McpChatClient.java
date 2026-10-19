package tech.dtkmn.examples.aimcp;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** A separate MCP consumer, created after the example server is listening. */
final class McpChatClient implements AutoCloseable {
    private final McpSyncClient mcp;
    private final ChatClient chat;
    private final List<ToolInvocation> toolInvocations = new ArrayList<>();

    McpChatClient(int port, ChatModel model) {
        var transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port)
            .endpoint("/mcp")
            .connectTimeout(Duration.ofSeconds(3))
            .requestBuilder(HttpRequest.newBuilder().timeout(Duration.ofSeconds(10)))
            .build();
        mcp = McpClient.sync(transport)
            .initializationTimeout(Duration.ofSeconds(10))
            .requestTimeout(Duration.ofSeconds(10)).build();
        try {
            mcp.initialize();
            var provider = SyncMcpToolCallbackProvider.builder().mcpClients(mcp)
                .toolFilter((connection, tool) -> tool.name().equals("lookup_customer")).build();
            ToolCallback[] callbacks = Arrays.stream(provider.getToolCallbacks())
                .map(this::recordResults).toArray(ToolCallback[]::new);
            if (callbacks.length != 1) {
                throw new IllegalStateException("Expected exactly one customer lookup tool");
            }
            var manager = DefaultToolCallingManager.builder()
                .maxCallsPerTool(2).maxTotalToolCalls(2)
                .onLimitExceeded(ToolCallLimitBehavior.THROW)
                .resolutionFallbackEnabled(false).build();
            chat = ChatClient.builder(model)
                .defaultAdvisors(ToolCallingAdvisor.builder().toolCallingManager(manager).build())
                .defaultTools((Object[]) callbacks).build();
        } catch (RuntimeException ex) {
            mcp.close();
            throw ex;
        }
    }

    ChatResponse ask(String question) {
        return chat.prompt()
            .system("Use the lookup tool for customer questions. Only report facts from its result. "
                + "If found=false, say the customer was not found. You cannot create, update or delete customers.")
            .user(question).call().chatResponse();
    }

    List<String> toolResults() {
        return toolInvocations.stream().map(ToolInvocation::result).toList();
    }

    List<ToolInvocation> toolInvocations() {
        return List.copyOf(toolInvocations);
    }

    private ToolCallback recordResults(ToolCallback delegate) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return delegate.getToolMetadata();
            }

            @Override
            public String call(String arguments) {
                String result = delegate.call(arguments);
                toolInvocations.add(new ToolInvocation(getToolDefinition().name(), arguments, result));
                return result;
            }

            @Override
            public String call(String arguments, ToolContext context) {
                String result = delegate.call(arguments, context);
                toolInvocations.add(new ToolInvocation(getToolDefinition().name(), arguments, result));
                return result;
            }
        };
    }

    record ToolInvocation(String toolName, String arguments, String result) {}

    @Override
    public void close() {
        mcp.close();
    }
}
