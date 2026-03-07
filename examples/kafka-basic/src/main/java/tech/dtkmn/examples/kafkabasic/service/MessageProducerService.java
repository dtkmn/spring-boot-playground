package tech.dtkmn.examples.kafkabasic.service;

import tech.dtkmn.examples.kafkabasic.model.MessageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MessageRequest request) {
        kafkaTemplate.send("basic-messages", request.key(), request.payload());
    }
}
