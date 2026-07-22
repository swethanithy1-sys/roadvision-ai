package com.roadvision.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsOverview(
        long totalReports,
        double resolutionRatePercent,
        BigDecimal averageConfidence,
        int roadSafetyIndex,
        List<SeverityCount> reportsBySeverity,
        List<MonthlyCount> monthlyReports,
        List<AreaCount> topAffectedAreas
) {
    public record SeverityCount(String severity, long count) {
    }

    public record MonthlyCount(String month, long count) {
    }

    public record AreaCount(String area, long count) {
    }
}
