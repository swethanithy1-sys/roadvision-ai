package com.roadvision.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** Constrains the Gemini Interactions API to return JSON matching the given schema. */
public record GeminiResponseFormat(
        String type,
        @JsonProperty("mime_type") String mimeType,
        Map<String, Object> schema
) {

    public static GeminiResponseFormat jsonSchema(Map<String, Object> schema) {
        return new GeminiResponseFormat("text", "application/json", schema);
    }
}
