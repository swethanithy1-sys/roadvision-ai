package com.roadvision.report;

import com.roadvision.common.constants.RepairPriority;
import jakarta.validation.constraints.NotNull;

public record ReportPriorityUpdateRequest(
        @NotNull(message = "Repair priority is required")
        RepairPriority repairPriority
) {
}
