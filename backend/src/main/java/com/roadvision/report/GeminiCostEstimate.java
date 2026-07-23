package com.roadvision.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Shape we ask the model to respond with (parsed from output_text). */
public record GeminiCostEstimate(
        @JsonProperty("estimated_cost_inr") double estimatedCostInr,
        String reasoning
) {
}
