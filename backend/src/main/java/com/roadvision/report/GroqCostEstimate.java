package com.roadvision.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Shape we ask the model to respond with (parsed from the chat completion's message content). */
public record GroqCostEstimate(
        @JsonProperty("estimated_cost_inr") double estimatedCostInr,
        String reasoning
) {
}
