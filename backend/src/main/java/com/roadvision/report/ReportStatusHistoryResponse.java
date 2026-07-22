package com.roadvision.report;

import com.roadvision.common.constants.ReportStatus;

import java.time.Instant;

public record ReportStatusHistoryResponse(
        ReportStatus status,
        String note,
        String changedByName,
        Instant changedAt
) {
}
