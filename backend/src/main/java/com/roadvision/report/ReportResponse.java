package com.roadvision.report;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.RepairPriority;
import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.constants.Severity;
import com.roadvision.detection.BoundingBox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String imageUrl,
        Double latitude,
        Double longitude,
        String addressText,
        String description,
        DamageType damageType,
        Severity severity,
        BigDecimal confidenceScore,
        List<BoundingBox> boundingBoxes,
        RepairPriority repairPriority,
        BigDecimal estimatedCost,
        ReportStatus status,
        String reporterName,
        Instant createdAt,
        Instant updatedAt
) {
}
