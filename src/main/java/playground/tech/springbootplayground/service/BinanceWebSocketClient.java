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
        logger.info("Starting WebSocket connection to: {}", wsUrl);
        connection = client.execute(
            URI.create(wsUrl),
            session -> {
                logger.info("WebSocket session established to: {}", wsUrl);
                return session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(msg -> logger.debug("Received message: {}", msg.substring(0, Math.min(100, msg.length()))))
                    .doOnNext(messageHandler)
                    .then();
            }
        )
        .transformDeferred(CircuitBreakerOperator.of(setupCircuitBreaker()))
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10))
            .jitter(0.5)
            .maxBackoff(Duration.ofMinutes(1))
            .doBeforeRetry(retrySignal -> logger.warn("Retrying connection... Attempt: {}", retrySignal.totalRetries() + 1))
        )
        .doOnError(error -> logger.error("Failed to establish WebSocket connection: ", error))
        .doOnSubscribe(sub -> logger.info("Subscribing to WebSocket: {}", wsUrl))
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
