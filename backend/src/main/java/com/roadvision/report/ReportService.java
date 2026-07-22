package com.roadvision.report;

import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.exception.ResourceNotFoundException;
import com.roadvision.detection.AIDetectionService;
import com.roadvision.detection.DetectionResult;
import com.roadvision.storage.FileStorageService;
import com.roadvision.user.User;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String UPLOAD_SUBDIRECTORY = "reports";

    private final ReportRepository reportRepository;
    private final ReportStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final AIDetectionService aiDetectionService;
    private final FileStorageService fileStorageService;
    private final RepairEstimator repairEstimator;
    private final ReportMapper reportMapper;

    @Transactional
    public ReportResponse submit(UUID reporterId, ReportSubmitRequest request, MultipartFile image) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        byte[] imageBytes = readBytes(image);
        DetectionResult detection = aiDetectionService.detect(imageBytes);
        String imagePath = fileStorageService.store(imageBytes, image.getOriginalFilename(), UPLOAD_SUBDIRECTORY);

        Report report = new Report(
                reporter,
                imagePath,
                request.latitude(),
                request.longitude(),
                request.addressText(),
                request.description(),
                detection.damageType(),
                detection.severity(),
                detection.confidenceScore(),
                detection.boundingBoxes(),
                repairEstimator.estimatePriority(detection.severity()),
                repairEstimator.estimateCost(detection.damageType(), detection.severity())
        );

        report = reportRepository.save(report);
        statusHistoryRepository.save(new ReportStatusHistory(report, ReportStatus.SUBMITTED, "Report submitted by citizen", reporter));

        return reportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> listMine(UUID reporterId, Pageable pageable) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable)
                .map(reportMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(UUID reportId, UUID requesterId, boolean requesterIsAdmin) {
        Report report = findAccessibleReport(reportId, requesterId, requesterIsAdmin);
        return reportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> listAll(Pageable pageable, ReportStatus statusFilter) {
        Page<Report> page = statusFilter == null
                ? reportRepository.findAll(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(statusFilter, pageable);
        return page.map(reportMapper::toResponse);
    }

    @Transactional
    public ReportResponse updateStatus(UUID reportId, UUID adminId, ReportStatusUpdateRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        report.setStatus(request.status());
        if (request.status() == ReportStatus.ASSIGNED) {
            report.setAssignedTo(admin);
        }
        report = reportRepository.save(report);

        statusHistoryRepository.save(new ReportStatusHistory(report, request.status(), request.note(), admin));

        return reportMapper.toResponse(report);
    }

    @Transactional
    public ReportResponse updatePriority(UUID reportId, ReportPriorityUpdateRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setRepairPriority(request.repairPriority());
        report = reportRepository.save(report);

        return reportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReportMapMarker> listMapMarkers() {
        return reportRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull().stream()
                .map(reportMapper::toMapMarker)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<ReportStatusHistoryResponse> getTimeline(UUID reportId, UUID requesterId, boolean requesterIsAdmin) {
        findAccessibleReport(reportId, requesterId, requesterIsAdmin);

        return statusHistoryRepository.findByReportIdOrderByChangedAtAsc(reportId).stream()
                .map(h -> new ReportStatusHistoryResponse(
                        h.getStatus(),
                        h.getNote(),
                        h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System",
                        h.getChangedAt()
                ))
                .toList();
    }

    private Report findAccessibleReport(UUID reportId, UUID requesterId, boolean requesterIsAdmin) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!requesterIsAdmin && !report.getReporter().getId().equals(requesterId)) {
            throw new AccessDeniedException("You do not have access to this report");
        }

        return report;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded image", e);
        }
    }
}
