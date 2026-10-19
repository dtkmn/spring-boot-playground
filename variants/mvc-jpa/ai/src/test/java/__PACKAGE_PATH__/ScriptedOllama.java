package __PACKAGE_NAME__;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A local protocol fixture: the application still uses its real Ollama client. */
final class ScriptedOllama implements AutoCloseable {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final List<JsonNode> responses = new CopyOnWriteArrayList<>();
    private volatile Function<JsonNode, Reply> script = request -> intent("CLARIFY");

    ScriptedOllama() { this(null); }

    /** Capture real protocol evidence without enabling application prompt or tool-result logging. */
    ScriptedOllama(String upstreamBaseUrl) {
        if (upstreamBaseUrl != null) {
            HttpClient upstream = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            script = request -> forward(upstream, upstreamBaseUrl, request);
        }
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        server.setExecutor(executor);
        server.createContext("/api/chat", exchange -> {
            try (exchange) {
                JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
                requests.add(request);
                Reply reply = script.apply(request);
                responses.add(JSON.readTree(reply.body()));
                byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(reply.status(), body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
                // A deadline test intentionally closes the client while this server is responding.
            }
        });
        server.start();
    }

    String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    List<JsonNode> requests() { return List.copyOf(requests); }
    List<JsonNode> responses() { return List.copyOf(responses); }
    void resetCaptured() { requests.clear(); responses.clear(); }
    void script(Function<JsonNode, Reply> next) { resetCaptured(); script = next; }

    private static Reply forward(HttpClient client, String upstreamBaseUrl, JsonNode body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(upstreamBaseUrl + "/api/chat"))
            .timeout(Duration.ofSeconds(65)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Reply(502, "{\"error\":\"Test proxy interrupted\"}");
        } catch (IOException exception) {
            return new Reply(502, "{\"error\":\"Test proxy upstream unavailable\"}");
        }
    }

    static Reply tool(String name, Object orderId) {
        var arguments = JSON.createObjectNode().putPOJO("orderId", orderId);
        var function = JSON.createObjectNode().put("name", name).set("arguments", arguments);
        var call = JSON.createObjectNode().set("function", function);
        var message = JSON.createObjectNode().put("role", "assistant").put("content", "");
        message.set("tool_calls", JSON.createArrayNode().add(call));
        return response(message, "stop");
    }

    static Reply intent(String intent) {
        return content(JSON.createObjectNode().put("intent", intent).toString(), "stop");
    }

    static Reply content(String content, String reason) {
        return response(JSON.createObjectNode().put("role", "assistant").put("content", content), reason);
    }

    private static Reply response(JsonNode message, String reason) {
        var body = JSON.createObjectNode().put("model", "fixture-model")
            .put("created_at", "2026-10-11T00:00:00Z").put("done", true)
            .put("done_reason", reason).put("prompt_eval_count", 12).put("eval_count", 8);
        body.set("message", message);
        return new Reply(200, body.toString());
    }

    @Override public void close() { server.stop(0); executor.shutdownNow(); }
    record Reply(int status, String body) { }
}
