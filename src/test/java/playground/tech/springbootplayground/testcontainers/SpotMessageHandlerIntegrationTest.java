package playground.tech.springbootplayground.testcontainers;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.utility.DockerImageName;
import playground.tech.springbootplayground.service.SpotMessageHandler;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.WebsocketServerSpec;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@Testcontainers
public class SpotMessageHandlerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SpotMessageHandlerIntegrationTest.class);
    private static final String TOPIC = "crypto-prices";

    @Container
    private static final KafkaContainer kafkaContainer =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        String mockWsUrl = "ws://localhost:" + mockServerPort;
        registry.add("binance.spot.websocket.url", () -> mockWsUrl);
        registry.add("binance.futures.websocket.url", () -> mockWsUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        log.info("Dynamic properties set: WebSocket URL to {} and Kafka Bootstrap Servers to {}", mockWsUrl, kafkaContainer.getBootstrapServers());
    }

    @Autowired
    private SpotMessageHandler spotMessageHandler;

    private ConsumerFactory<String, String> consumerFactory;
    private Consumer<String, String> consumer;

    private static DisposableServer mockWebSocketServer;
    private static int mockServerPort;

    @BeforeAll
    static void startMockWebSocketServer() {
        mockWebSocketServer = HttpServer.create()
            .port(0) // Let the system assign an available port
            .route(routes ->
                routes.ws("/stream?streams=btcusdt@trade/ethusdt@trade", (in, out) -> {
                    log.info("Mock WebSocket: New client connection established.");
                    // Send a single predefined message to the client
                    String singleMessage = "{\"stream\":\"btcusdt@aggTrade\",\"data\":{\"s\":\"BTCUSDT\",\"p\":\"10000\",\"q\":\"0.5\",\"E\":1640995200000}}";
                    log.info("Mock WebSocket: Sending single message: {}", singleMessage);
                    return out.sendString(Flux.just(singleMessage)).then();
                }, WebsocketServerSpec.builder().build())
            )
            .bindNow();

        mockServerPort = mockWebSocketServer.port();
        log.info("Mock WebSocket Server started on port: {}", mockServerPort);

        // Create a Kafka topic
        Properties config = new Properties();
        config.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(config)) {
            adminClient.createTopics(List.of(
                new NewTopic(TOPIC, 1, (short) 1)
            )).all().get();
            log.info("Kafka topics created successfully, including: {}", TOPIC);
        } catch (Exception e) {
            log.error("Failed to create Kafka topics", e);
            throw new RuntimeException("Failed to create Kafka topic", e);
        }
    }

    @AfterAll
    static void stopMockWebSocketServer() {
        if (mockWebSocketServer != null) {
            mockWebSocketServer.disposeNow();
            log.info("Mock WebSocket Server stopped.");
        }
    }

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(),
                "testGroup-" + UUID.randomUUID(),
                "false");

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(List.of(TOPIC));
//        consumer.poll(Duration.ofMillis(500));
        log.info("Kafka Consumer created and subscribed to topic: {}", TOPIC);
    }

    @AfterEach
    void tearDown() {
        if (consumer!= null) {
            try {
                // Close the consumer with a reasonable timeout
                consumer.close(Duration.ofSeconds(5));
                log.info("Kafka Consumer closed successfully.");
            } catch (Exception e) {
                log.error("Error closing Kafka Consumer: {}", e.getMessage(), e);
                // Depending on the test policy, you might want to rethrow or handle
            }
        }
    }

    @Test
    void testHandleMessage() throws Exception {
        log.info("Attempting to receive a single record from topic {} with a 10s timeout.", TOPIC);
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(20));

        log.info("Received record: Key='{}', Value='{}'", record.key(), record.value());
        assertEquals("BTCUSDT", record.key());
        assertTrue(record.value().contains("\"p\":\"10000\""), "Record value should contain price.");
        assertTrue(record.value().contains("\"q\":\"0.5\""), "Record value should contain quantity.");
        log.info("Assertions on the received record passed.");
    }

}
