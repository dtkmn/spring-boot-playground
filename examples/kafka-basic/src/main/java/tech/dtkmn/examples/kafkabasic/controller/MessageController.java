package tech.dtkmn.examples.kafkabasic.controller;

import jakarta.validation.Valid;
import tech.dtkmn.examples.kafkabasic.model.MessageRequest;
import tech.dtkmn.examples.kafkabasic.service.MessageProducerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageProducerService messageProducerService;

    public MessageController(MessageProducerService messageProducerService) {
        this.messageProducerService = messageProducerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void publish(@Valid @RequestBody MessageRequest request) {
        messageProducerService.publish(request);
    }
}
