package playground.tech.springbootplayground.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;

public class BinanceWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketClient.class);
    private final String wsUrl;
    private final Consumer<String> messageHandler;
    private final WebSocketClient client;

    public BinanceWebSocketClient(String wsUrl, Consumer<String> messageHandler,
                                  WebSocketClient client) {
        this.wsUrl = wsUrl;
        this.messageHandler = messageHandler;
        this.client = client;
        startWebSocketConnection();
    }

    private void startWebSocketConnection() {
        client.execute(
            URI.create(wsUrl),
            session -> session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(messageHandler)
                .then()
        )
        .retryWhen(Retry.backoff(5, Duration.ofSeconds(5))
            .doBeforeRetry(retrySignal -> logger.warn("Retrying connection... Attempt: {}", retrySignal.totalRetries() + 1))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
        )
        .doOnError(error -> logger.error("Failed to establish WebSocket connection: ", error))
        .subscribe();
    }

}