package playground.tech.springbootplayground.testcontainers;



import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.utility.DockerImageName;
import playground.tech.springbootplayground.service.SpotMessageHandler;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.WebsocketServerSpec;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
public class SpotMessageHandlerIntegrationTest {

    private static final String TOPIC = "crypto-prices";

    @Container
    private static final KafkaContainer kafkaContainer =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));

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
                    System.out.println("New WebSocket connection: " + in.receive().toString());
                    // Send a predefined message to the client
                    return out.sendString(
                        Flux.interval(Duration.ofSeconds(1))
                            .map(seq -> "{\"stream\":\"btcusdt@aggTrade\",\"data\":{\"s\":\"BTCUSDT\",\"p\":\"10000\",\"q\":\"0.5\",\"E\":1640995200000}}")
                    ).then();
                }, WebsocketServerSpec.builder().build())
            )
            .bindNow();

        mockServerPort = mockWebSocketServer.port();

        // Create Kafka topic
        Properties config = new Properties();
        config.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(config)) {
            adminClient.createTopics(List.of(
                new NewTopic(TOPIC, 1, (short) 1),
                new NewTopic("futures-funding-rate", 1, (short) 1),
                new NewTopic("moving-average-topic", 1, (short) 1),
                new NewTopic("tweets", 1, (short) 1),
                new NewTopic("reddit-posts", 1, (short) 1),
                new NewTopic("crypto-symbols", 1, (short) 1),
                new NewTopic("alerts", 1, (short) 1)
            )).all().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Kafka topic", e);
        }
    }

    @AfterAll
    static void stopMockWebSocketServer() {
        if (mockWebSocketServer != null) {
            mockWebSocketServer.disposeNow();
        }
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // Set the Binance WebSocket URL to point to the mock server
        String mockWsUrl = "ws://localhost:" + mockServerPort;
        registry.add("binance.spot.websocket.url", () -> mockWsUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }


    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "testGroup-" + UUID.randomUUID(), "true");

        consumerProps.put("key.deserializer", StringDeserializer.class);
        consumerProps.put("value.deserializer", StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(List.of(TOPIC));
        consumer.poll(Duration.ofMillis(100));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void testHandleMessage() throws Exception {
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, TOPIC, Duration.ofSeconds(5));
        assertEquals("BTCUSDT", record.key());
        assertTrue(record.value().contains("\"p\":\"10000\""));
        assertTrue(record.value().contains("\"q\":\"0.5\""));
    }

}
