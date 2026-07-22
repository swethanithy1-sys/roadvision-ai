package com.roadvision.workorder;

import com.roadvision.common.exception.ResourceNotFoundException;
import com.roadvision.report.RepairEstimator;
import com.roadvision.report.Report;
import com.roadvision.report.ReportRepository;
import com.roadvision.user.User;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Isolated in its own bean (not just a method on WorkOrderService) so its
 * {@code REQUIRES_NEW} transaction is genuinely a separate physical transaction — required
 * for {@link WorkOrderService} to safely catch a unique-constraint violation here and fall
 * back to reading the winning row without the caller's own transaction being poisoned by
 * the failed insert (a call from `this.` inside the same class would bypass Spring's proxy
 * and silently ignore this propagation setting).
 */
@Component
@RequiredArgsConstructor
class WorkOrderWriter {

    private final ReportRepository reportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final RepairEstimator repairEstimator;
    private final WorkOrderMapper workOrderMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkOrderResponse create(UUID reportId, UUID adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WorkOrder workOrder = new WorkOrder(
                report,
                repairEstimator.estimateMaterials(report.getDamageType()),
                repairEstimator.estimateLaborHours(report.getSeverity()),
                repairEstimator.estimateDurationDays(report.getSeverity()),
                admin
        );

        // Flush (not just save) so a unique-constraint violation surfaces here, inside this
        // isolated transaction, rather than at an unpredictable later flush point.
        WorkOrder saved = workOrderRepository.saveAndFlush(workOrder);
        return workOrderMapper.toResponse(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<WorkOrderResponse> fetchExisting(UUID reportId) {
        return workOrderRepository.findByReportId(reportId).map(workOrderMapper::toResponse);
    }
}
