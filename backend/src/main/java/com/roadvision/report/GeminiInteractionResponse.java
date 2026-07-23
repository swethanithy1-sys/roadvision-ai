package com.roadvision.report;

import java.util.List;

/**
 * Only the fields needed to locate the model's text output; ignores id/usage/created/etc.
 * The Interactions API nests the actual reply inside {@code steps[]} — the step with
 * {@code type: "model_output"} holds a {@code content[]} array of text parts.
 */
public record GeminiInteractionResponse(List<GeminiStep> steps) {

    public record GeminiStep(String type, List<GeminiContentPart> content) {
    }

    public record GeminiContentPart(String type, String text) {
    }
}
