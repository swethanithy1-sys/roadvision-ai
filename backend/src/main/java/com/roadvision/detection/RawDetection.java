package com.roadvision.detection;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire shape returned by the Python detection service — one box per raw model detection. */
public record RawDetection(
        @JsonProperty("class_name") String className,
        double confidence,
        double x,
        double y,
        double width,
        double height
) {
}
