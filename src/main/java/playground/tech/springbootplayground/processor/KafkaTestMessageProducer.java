package playground.tech.springbootplayground.processor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTestMessageProducer implements CommandLineRunner {

    private KafkaTemplate<String, String> kafkaTemplate;

    public KafkaTestMessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        kafkaTemplate.send("my-topic", "Test message at startup");
        System.out.println("Test message sent to my-topic");
    }
}
