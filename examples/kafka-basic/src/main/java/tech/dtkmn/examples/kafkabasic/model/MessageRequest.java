package tech.dtkmn.examples.kafkabasic.model;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
    @NotBlank String key,
    @NotBlank String payload
) {
}
