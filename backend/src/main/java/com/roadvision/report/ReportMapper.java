package com.roadvision.report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    private final String uploadsPublicPath;

    public ReportMapper(@Value("${app.storage.public-path:/uploads}") String uploadsPublicPath) {
        this.uploadsPublicPath = uploadsPublicPath;
    }

    public ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                uploadsPublicPath + "/" + report.getImagePath(),
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
                uploadsPublicPath + "/" + report.getImagePath(),
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
