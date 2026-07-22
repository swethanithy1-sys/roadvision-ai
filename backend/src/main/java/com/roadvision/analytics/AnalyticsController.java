package com.roadvision.analytics;

import com.roadvision.common.response.ApiResponse;
import com.roadvision.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/citizen-summary")
    public ApiResponse<CitizenDashboardSummary> citizenSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(analyticsService.getCitizenSummary(principal.getId()));
    }

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverview> overview() {
        return ApiResponse.success(analyticsService.getOverview());
    }
}
