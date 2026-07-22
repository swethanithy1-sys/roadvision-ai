package com.roadvision.report;

public record GroqResponseFormat(String type) {
    public static GroqResponseFormat jsonObject() {
        return new GroqResponseFormat("json_object");
    }
}
