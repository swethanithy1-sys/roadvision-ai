package com.roadvision.workorder;

import com.roadvision.common.exception.ResourceNotFoundException;
import com.roadvision.report.RepairEstimator;
import com.roadvision.report.Report;
import com.roadvision.report.ReportMapper;
import com.roadvision.report.ReportRepository;
import com.roadvision.user.User;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final ReportRepository reportRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final RepairEstimator repairEstimator;
    private final ReportMapper reportMapper;

    @Transactional
    public WorkOrderResponse generateOrFetch(UUID reportId, UUID adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return workOrderRepository.findByReportId(reportId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    User admin = userRepository.findById(adminId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                    WorkOrder workOrder = new WorkOrder(
                            report,
                            repairEstimator.estimateMaterials(report.getDamageType()),
                            repairEstimator.estimateLaborHours(report.getSeverity()),
                            repairEstimator.estimateDurationDays(report.getSeverity()),
                            admin
                    );

                    return toResponse(workOrderRepository.save(workOrder));
                });
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getByReportId(UUID reportId) {
        WorkOrder workOrder = workOrderRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("No work order exists for this report yet"));
        return toResponse(workOrder);
    }

    private WorkOrderResponse toResponse(WorkOrder workOrder) {
        return new WorkOrderResponse(
                workOrder.getId(),
                reportMapper.toResponse(workOrder.getReport()),
                workOrder.getMaterialsRequired(),
                workOrder.getEstimatedLaborHours(),
                workOrder.getEstimatedDurationDays(),
                workOrder.getGeneratedBy() != null ? workOrder.getGeneratedBy().getFullName() : "System",
                workOrder.getGeneratedAt()
        );
    }
}
