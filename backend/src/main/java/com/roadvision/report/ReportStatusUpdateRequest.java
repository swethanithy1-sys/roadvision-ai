package com.roadvision.report;

import com.roadvision.common.constants.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportStatusUpdateRequest(
        @NotNull(message = "Status is required")
        ReportStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
