package com.roadvision.analytics;

import com.roadvision.report.ReportResponse;

import java.util.List;

public record CitizenDashboardSummary(
        long totalReports,
        long pendingRepairs,
        long resolvedReports,
        long highSeverityReports,
        int roadSafetyScore,
        List<ReportResponse> recentActivity
) {
}
