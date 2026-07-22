package com.roadvision.report;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.RepairPriority;
import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.constants.Severity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public-facing marker for the community hazard map — deliberately omits the reporter's
 * identity, keeping crowd-sourced hazard visibility separate from the per-owner access
 * control on {@link ReportResponse} / GET /reports/{id}.
 */
public record ReportMapMarker(
        UUID id,
        String imageUrl,
        Double latitude,
        Double longitude,
        String addressText,
        DamageType damageType,
        Severity severity,
        BigDecimal confidenceScore,
        RepairPriority repairPriority,
        BigDecimal estimatedCost,
        ReportStatus status,
        Instant createdAt
) {
}
