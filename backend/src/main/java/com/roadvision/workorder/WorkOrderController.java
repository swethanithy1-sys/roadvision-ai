package com.roadvision.workorder;

import com.roadvision.common.response.ApiResponse;
import com.roadvision.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reports/{reportId}/work-order")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderResponse>> generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID reportId
    ) {
        WorkOrderResponse response = workOrderService.generateOrFetch(reportId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Work order ready", response));
    }

    @GetMapping
    public ApiResponse<WorkOrderResponse> get(@PathVariable UUID reportId) {
        return ApiResponse.success(workOrderService.getByReportId(reportId));
    }
}
