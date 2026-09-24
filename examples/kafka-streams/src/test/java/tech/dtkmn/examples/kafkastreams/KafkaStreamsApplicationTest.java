package tech.dtkmn.examples.kafkastreams;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import tech.dtkmn.examples.kafkastreams.entity.TradeAggregate;
import tech.dtkmn.examples.kafkastreams.service.TradeStreamProcessingService;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.streams.auto-startup=false")
class KafkaStreamsApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilderFactory;

    @Autowired
    private JsonMapper jsonMapper;

    @TempDir
    private Path stateDirectory;

    @Test
    void shouldCreateTheApplicationAndTopologyWithoutConnectingToKafka() {
        assertThat(context.getBean(TradeStreamProcessingService.class)).isNotNull();
        assertThat(streamsBuilderFactory.getTopology()).isNotNull();
        assertThat(streamsBuilderFactory.isRunning()).isFalse();
    }

    @Test
    void shouldAggregateTradesThroughJackson3StateAndOutputSerialization() {
        Properties properties = new Properties();
        properties.putAll(streamsBuilderFactory.getStreamsConfiguration());
        properties.put(StreamsConfig.STATE_DIR_CONFIG, stateDirectory.toString());
        properties.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);

        try (var driver = new TopologyTestDriver(streamsBuilderFactory.getTopology(), properties)) {
            var input = driver.createInputTopic("crypto-prices", new StringSerializer(), new StringSerializer(),
                Instant.parse("2026-01-01T00:00:00Z"), Duration.ofSeconds(1));
            var output = driver.createOutputTopic("moving-average-topic", new StringDeserializer(),
                new ByteArrayDeserializer());

            input.pipeInput("BTCUSDT", """
                {"s":"BTCUSDT","p":"100.0","q":"2.0","E":1767225600000}
                """);
            input.pipeInput("BTCUSDT", """
                {"s":"BTCUSDT","p":"150.0","q":"1.0","E":1767225601000}
                """);

            var first = output.readKeyValue();
            assertThat(first.key).isEqualTo("BTCUSDT");
            TradeAggregate initial = jsonMapper.readValue(first.value, TradeAggregate.class);
            assertThat(initial.getTotalPrice()).isEqualTo(100.0);
            assertThat(initial.getTradeCount()).isEqualTo(1);

            var second = output.readKeyValue();
            assertThat(second.key).isEqualTo("BTCUSDT");
            var json = jsonMapper.readTree(second.value);
            assertThat(json.get("totalPrice").asDouble()).isEqualTo(250.0);
            assertThat(json.get("tradeCount").asLong()).isEqualTo(2);
            TradeAggregate restored = jsonMapper.readValue(second.value, TradeAggregate.class);
            assertThat(restored.getTotalPrice()).isEqualTo(250.0);
            assertThat(restored.getTradeCount()).isEqualTo(2);
            assertThat(output.isEmpty()).isTrue();
        }
    }
}
