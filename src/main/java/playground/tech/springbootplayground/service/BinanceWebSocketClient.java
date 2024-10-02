package playground.tech.springbootplayground.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;

public class BinanceWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketClient.class);
    private final String wsUrl;
    private final Consumer<String> messageHandler;
    private final WebSocketClient client;
    private Disposable connection;

    public BinanceWebSocketClient(String wsUrl, Consumer<String> messageHandler,
                                  WebSocketClient client) {
        this.wsUrl = wsUrl;
        this.messageHandler = messageHandler;
        this.client = client;
        startWebSocketConnection();
    }

    private CircuitBreaker setupCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Percentage
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(10)
            .build();
        return CircuitBreaker.of("websocketCircuitBreaker", config);
    }

    private void startWebSocketConnection() {
        connection = client.execute(
            URI.create(wsUrl),
            session -> session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(messageHandler)
                .then()
        )
        .transformDeferred(CircuitBreakerOperator.of(setupCircuitBreaker()))
        .retryWhen(Retry.backoff(5, Duration.ofSeconds(10))
            .jitter(0.5)
            .maxBackoff(Duration.ofMinutes(1))
            .doBeforeRetry(retrySignal -> logger.warn("Retrying connection... Attempt: {}", retrySignal.totalRetries() + 1))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
        )
        .doOnError(error -> logger.error("Failed to establish WebSocket connection: ", error))
        .subscribe();
    }

    @PreDestroy
    public void shutdownWebSocketConnection() {
        if (connection != null && !connection.isDisposed()) {
            logger.info("Closing WebSocket connection...");
            connection.dispose();
        }
    }

}