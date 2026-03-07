package tech.dtkmn.examples.kafkabasic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    @KafkaListener(topics = "basic-messages", groupId = "kafka-basic-example")
    public void consume(String payload) {
        log.info("Consumed message={}", payload);
    }
}
