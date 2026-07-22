package com.roadvision.workorder;

import com.roadvision.report.ReportResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkOrderResponse(
        UUID id,
        ReportResponse report,
        List<String> materialsRequired,
        BigDecimal estimatedLaborHours,
        BigDecimal estimatedDurationDays,
        String generatedByName,
        Instant generatedAt
) {
}
