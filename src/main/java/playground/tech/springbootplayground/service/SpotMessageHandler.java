package playground.tech.springbootplayground.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
            log.info("Receiving message: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode dataNode = jsonNode.get("data");
            String symbol = dataNode.get("s").asText();
            kafkaTemplate.send("crypto-prices", symbol, dataNode.toString())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to deliver message [{}]. Error: {}", dataNode, ex.getMessage());
                    }
                    else {
                        log.info("Message delivered successfully: {}", dataNode);
                    }
                });
        } catch (Exception e) {
            log.error("Failed to deliver message [{}]. Error: {}", message, e.getMessage());
        }
    }
}
