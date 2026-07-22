package com.roadvision.analytics;

import com.roadvision.report.ReportResponse;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardSummary(
        long totalReports,
        long pendingReports,
        long highPriorityRepairs,
        BigDecimal totalEstimatedCost,
        List<ReportResponse> recentReports
) {
}
