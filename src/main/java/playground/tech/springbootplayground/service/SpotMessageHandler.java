package playground.tech.springbootplayground.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SpotMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpotMessageHandler.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpotMessageHandler(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handleMessage(String message) {
        try {
            // Log the message
            logger.info("Received message from WebSocket: {}", message);
            JsonNode jsonNode = objectMapper.readTree(message);
            String stream = jsonNode.get("stream").asText();
            JsonNode dataNode = jsonNode.get("data");
            String symbol = dataNode.get("s").asText();
            kafkaTemplate.send("crypto-prices", symbol, dataNode.toString());
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message: {}", message, e);
        }
    }
}
