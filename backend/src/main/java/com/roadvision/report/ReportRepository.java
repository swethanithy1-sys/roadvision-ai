package com.roadvision.report;

import com.roadvision.common.constants.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    Page<Report> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    List<Report> findByReporterId(UUID reporterId);

    List<Report> findByLatitudeIsNotNullAndLongitudeIsNotNull();
}
