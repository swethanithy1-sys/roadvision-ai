package com.roadvision.report;

import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getImagePath(),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddressText(),
                report.getDescription(),
                report.getDamageType(),
                report.getSeverity(),
                report.getConfidenceScore(),
                report.getBoundingBoxes(),
                report.getRepairPriority(),
                report.getEstimatedCost(),
                report.getStatus(),
                report.getReporter().getFullName(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    public ReportMapMarker toMapMarker(Report report) {
        return new ReportMapMarker(
                report.getId(),
                report.getImagePath(),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddressText(),
                report.getDamageType(),
                report.getSeverity(),
                report.getConfidenceScore(),
                report.getRepairPriority(),
                report.getEstimatedCost(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
