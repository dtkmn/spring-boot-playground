package playground.tech.springbootplayground.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FuturesMessageHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FuturesMessageHandler(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            JsonNode dataNode = jsonNode.get("data");
            String symbol = dataNode.get("s").asText();
            kafkaTemplate.send("futures-funding-rate", symbol, dataNode.toString())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to deliver message [{}]. Error: {}", dataNode, ex.getMessage());
                    }
                });
        } catch (Exception e) {
            log.error("Failed to deliver message [{}]. Error: {}", message, e.getMessage());
        }
    }
}
