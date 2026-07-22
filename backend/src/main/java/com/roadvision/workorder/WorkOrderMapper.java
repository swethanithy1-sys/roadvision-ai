package com.roadvision.workorder;

import com.roadvision.report.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkOrderMapper {

    private final ReportMapper reportMapper;

    public WorkOrderResponse toResponse(WorkOrder workOrder) {
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
