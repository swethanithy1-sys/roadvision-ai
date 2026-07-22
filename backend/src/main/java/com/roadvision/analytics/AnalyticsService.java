package com.roadvision.analytics;

import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.constants.Severity;
import com.roadvision.report.Report;
import com.roadvision.report.ReportMapper;
import com.roadvision.report.ReportRepository;
import com.roadvision.report.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Set<ReportStatus> OPEN_STATUSES = java.util.EnumSet.of(
            ReportStatus.SUBMITTED, ReportStatus.UNDER_REVIEW, ReportStatus.ASSIGNED, ReportStatus.IN_PROGRESS
    );

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;

    @Transactional(readOnly = true)
    public CitizenDashboardSummary getCitizenSummary(UUID reporterId) {
        List<Report> reports = reportRepository.findByReporterId(reporterId);

        long total = reports.size();
        long pending = reports.stream().filter(r -> OPEN_STATUSES.contains(r.getStatus())).count();
        long resolved = reports.stream().filter(r -> r.getStatus() == ReportStatus.RESOLVED).count();
        long highSeverity = reports.stream().filter(r -> r.getSeverity() == Severity.HIGH).count();

        List<ReportResponse> recentActivity = reports.stream()
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .limit(5)
                .map(reportMapper::toResponse)
                .toList();

        return new CitizenDashboardSummary(total, pending, resolved, highSeverity, roadSafetyScore(reports), recentActivity);
    }

    @Transactional(readOnly = true)
    public AnalyticsOverview getOverview() {
        List<Report> reports = reportRepository.findAll();

        long total = reports.size();

        long resolved = reports.stream().filter(r -> r.getStatus() == ReportStatus.RESOLVED).count();
        double resolutionRate = total == 0 ? 0.0 : Math.round((resolved * 1000.0) / total) / 10.0;

        BigDecimal averageConfidence = total == 0
                ? BigDecimal.ZERO
                : reports.stream()
                        .map(Report::getConfidenceScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        List<AnalyticsOverview.SeverityCount> bySeverity = reports.stream()
                .collect(Collectors.groupingBy(r -> r.getSeverity().name(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new AnalyticsOverview.SeverityCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AnalyticsOverview.SeverityCount::severity))
                .toList();

        List<AnalyticsOverview.MonthlyCount> monthly = reports.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().atZone(ZoneOffset.UTC).format(MONTH_FORMAT),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new AnalyticsOverview.MonthlyCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AnalyticsOverview.MonthlyCount::month))
                .toList();

        List<AnalyticsOverview.AreaCount> topAreas = reports.stream()
                .map(r -> r.getAddressText() == null || r.getAddressText().isBlank() ? "Unspecified" : r.getAddressText().trim())
                .collect(Collectors.groupingBy(area -> area, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new AnalyticsOverview.AreaCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(AnalyticsOverview.AreaCount::count, Comparator.reverseOrder()))
                .limit(5)
                .toList();

        return new AnalyticsOverview(total, resolutionRate, averageConfidence, roadSafetyScore(reports), bySeverity, monthly, topAreas);
    }

    /**
     * 100 minus the average severity penalty of currently unresolved hazards
     * (LOW=5, MEDIUM=15, HIGH=30) — resolved/rejected reports no longer count
     * as live road hazards, so they don't drag the score down.
     */
    private int roadSafetyScore(List<Report> reports) {
        List<Report> openHazards = reports.stream()
                .filter(r -> r.getStatus() != ReportStatus.RESOLVED && r.getStatus() != ReportStatus.REJECTED)
                .toList();

        if (openHazards.isEmpty()) {
            return 100;
        }

        double averagePenalty = openHazards.stream()
                .mapToInt(this::severityPenalty)
                .average()
                .orElse(0);

        int score = (int) Math.round(100 - averagePenalty);
        return Math.max(0, Math.min(100, score));
    }

    private int severityPenalty(Report report) {
        return switch (report.getSeverity()) {
            case LOW -> 5;
            case MEDIUM -> 15;
            case HIGH -> 30;
        };
    }
}
