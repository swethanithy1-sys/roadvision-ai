package com.roadvision.report;

import java.util.List;

public record GroqChatResponse(List<GroqChoice> choices) {

    public record GroqChoice(GroqChatMessage message) {
    }
}
