package playground.tech.springbootplayground.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FuturesMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(FuturesMessageHandler.class);
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
                        logger.error("Failed to deliver message [{}]. Error: {}", dataNode, ex.getMessage());
                    }
                });
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message: {}", message, e);
        }
    }
}
