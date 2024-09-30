package playground.tech.springbootplayground.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Service;
import playground.tech.springbootplayground.entity.TradeAggregate;
import playground.tech.springbootplayground.entity.TradeEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class TradeStreamProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(TradeStreamProcessingService.class);
    private final ObjectMapper objectMapper;

    public TradeStreamProcessingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    public void buildPipeline(StreamsBuilder builder) {

        // Instantiate the Serde for TradeAggregate
        Serde<TradeAggregate> tradeAggregateSerde = new JsonSerde<>(TradeAggregate.class);

        // Configure the Serde
        Map<String, Object> serdeConfigs = new HashMap<>();
        serdeConfigs.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        tradeAggregateSerde.configure(serdeConfigs, false);

        KStream<String, String> sourceStream = builder.stream("crypto-prices");
        sourceStream.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1)))
            .aggregate(
                TradeAggregate::new,
                (key, tradeJson, aggregate) -> {
                    TradeEvent trade = parseTradeEvent(tradeJson);
                    if (trade != null) {
                        aggregate.add(trade);
                    }
                    return aggregate;
                },
                Materialized.with(Serdes.String(), tradeAggregateSerde)
            )
            .toStream()
            .map((windowedKey, aggregate) -> KeyValue.pair(windowedKey.key(), aggregate))
            .to("moving-average-topic", Produced.with(Serdes.String(), tradeAggregateSerde));
    }

    private TradeEvent parseTradeEvent(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return new TradeEvent(
                node.get("s").asText(),
                node.get("p").asDouble(),
                node.get("q").asDouble(),
                node.get("E").asLong()
            );
        } catch (Exception e) {
            logger.error("Failed to parse trade event: {}", json, e);
            return null;
        }
    }
}
