package com.roadvision.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Only the convenience field we need; the API also returns id/steps which we ignore. */
public record GeminiInteractionResponse(
        @JsonProperty("output_text") String outputText
) {
}
