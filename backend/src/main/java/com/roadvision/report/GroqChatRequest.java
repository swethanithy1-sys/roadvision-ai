package com.roadvision.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GroqChatRequest(
        String model,
        List<GroqChatMessage> messages,
        double temperature,
        @JsonProperty("response_format") GroqResponseFormat responseFormat
) {
}
