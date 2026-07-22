package com.roadvision.workorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    Optional<WorkOrder> findByReportId(UUID reportId);
}
