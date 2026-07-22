package com.roadvision.detection;

import java.util.List;

public record RoboflowDetectionResponse(List<RoboflowPrediction> predictions, RoboflowImageInfo image) {
}
