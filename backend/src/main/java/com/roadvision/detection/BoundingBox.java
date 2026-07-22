package com.roadvision.detection;

/**
 * Normalized (0.0–1.0) bounding box coordinates relative to image width/height,
 * so the frontend can render the overlay at any displayed image size.
 */
public record BoundingBox(
        double x,
        double y,
        double width,
        double height,
        String label,
        double confidence
) {
}
