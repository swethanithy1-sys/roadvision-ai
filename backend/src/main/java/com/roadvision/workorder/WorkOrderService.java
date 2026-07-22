package com.roadvision.workorder;

import com.roadvision.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderWriter workOrderWriter;
    private final WorkOrderMapper workOrderMapper;

    @Transactional(readOnly = true)
    public WorkOrderResponse generateOrFetch(UUID reportId, UUID adminId) {
        return workOrderRepository.findByReportId(reportId)
                .map(workOrderMapper::toResponse)
                .orElseGet(() -> createOrRecoverFromRace(reportId, adminId));
    }

    /**
     * Two admins (or two React StrictMode double-invocations of the same request) can both
     * see "no work order yet" before either commits. workOrderWriter.create() runs in its own
     * isolated transaction, so if it loses the race to the DB's unique constraint, that
     * transaction rolls back cleanly and we just fetch the winner's row in a fresh one — no
     * raw SQL exception should ever reach the controller.
     */
    private WorkOrderResponse createOrRecoverFromRace(UUID reportId, UUID adminId) {
        try {
            return workOrderWriter.create(reportId, adminId);
        } catch (DataIntegrityViolationException ex) {
            return workOrderWriter.fetchExisting(reportId)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getByReportId(UUID reportId) {
        return workOrderRepository.findByReportId(reportId)
                .map(workOrderMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No work order exists for this report yet"));
    }
}
