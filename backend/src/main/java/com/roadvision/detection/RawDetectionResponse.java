package com.roadvision.detection;

import java.util.List;

public record RawDetectionResponse(List<RawDetection> detections) {
}
