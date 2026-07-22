package com.roadvision.detection;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.Severity;

import java.math.BigDecimal;
import java.util.List;

public record DetectionResult(
        DamageType damageType,
        Severity severity,
        BigDecimal confidenceScore,
        List<BoundingBox> boundingBoxes
) {
}
