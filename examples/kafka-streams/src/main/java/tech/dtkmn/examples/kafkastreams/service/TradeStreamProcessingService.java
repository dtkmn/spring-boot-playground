package tech.dtkmn.examples.kafkastreams.service;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import org.springframework.stereotype.Service;
import tech.dtkmn.examples.kafkastreams.entity.TradeAggregate;
import tech.dtkmn.examples.kafkastreams.entity.TradeEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class TradeStreamProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(TradeStreamProcessingService.class);
    private final JsonMapper jsonMapper;

    public TradeStreamProcessingService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Autowired
    public void buildPipeline(StreamsBuilder builder) {

        // Instantiate the Serde for TradeAggregate
        Serde<TradeAggregate> tradeAggregateSerde = new JacksonJsonSerde<>(TradeAggregate.class, jsonMapper);

        // Configure the Serde
        Map<String, Object> serdeConfigs = new HashMap<>();
        serdeConfigs.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
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
            JsonNode node = jsonMapper.readTree(json);
            return new TradeEvent(
                node.get("s").asString(),
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
