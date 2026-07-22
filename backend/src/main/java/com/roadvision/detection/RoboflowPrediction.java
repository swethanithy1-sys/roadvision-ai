package com.roadvision.detection;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Roboflow's hosted-inference prediction shape: center-point, pixel-space coordinates. */
public record RoboflowPrediction(
        double x,
        double y,
        double width,
        double height,
        double confidence,
        @JsonProperty("class") String className
) {
}
