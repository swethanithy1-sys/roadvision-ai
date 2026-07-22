package com.roadvision.report;

import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.response.ApiResponse;
import com.roadvision.common.response.PageResponse;
import com.roadvision.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ReportResponse>> submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("image") MultipartFile image,
            @Valid @RequestPart("report") ReportSubmitRequest request
    ) {
        ReportResponse response = reportService.submit(principal.getId(), request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report submitted and analyzed successfully", response));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ReportResponse>> listMine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(PageResponse.from(reportService.listMine(principal.getId(), pageable)));
    }

    @GetMapping("/map")
    public ApiResponse<List<ReportMapMarker>> map() {
        return ApiResponse.success(reportService.listMapMarkers());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ReportResponse>> listAll(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(PageResponse.from(reportService.listAll(pageable, status)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReportStatusUpdateRequest request
    ) {
        return ApiResponse.success("Status updated", reportService.updateStatus(id, principal.getId(), request));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportResponse> updatePriority(
            @PathVariable UUID id,
            @Valid @RequestBody ReportPriorityUpdateRequest request
    ) {
        return ApiResponse.success("Priority updated", reportService.updatePriority(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ApiResponse.success(reportService.getById(id, principal.getId(), isAdmin));
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<List<ReportStatusHistoryResponse>> getTimeline(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ApiResponse.success(reportService.getTimeline(id, principal.getId(), isAdmin));
    }
}
