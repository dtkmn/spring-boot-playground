package playground.tech.springbootplayground.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpotMessageHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpotMessageHandler(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode dataNode = jsonNode.get("data");
            if (dataNode == null || dataNode.get("s") == null) {
                log.debug("Skipping message without data.symbol field: {}", message);
                return;
            }
            String symbol = dataNode.get("s").asText();
            log.info("Received spot trade message for symbol={}", symbol);
            kafkaTemplate.send("crypto-prices", symbol, dataNode.toString())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to deliver message [{}]", dataNode, ex);
                    }
                    else {
                        log.info("Message delivered successfully for symbol={}", symbol);
                    }
                });
        } catch (Exception e) {
            log.error("Failed to process spot message [{}]", message, e);
        }
    }
}
