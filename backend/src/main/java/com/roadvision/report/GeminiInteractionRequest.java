package com.roadvision.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiInteractionRequest(
        String model,
        @JsonProperty("system_instruction") String systemInstruction,
        String input,
        @JsonProperty("response_format") GeminiResponseFormat responseFormat
) {
}
