package com.marinbom2.retailfnbplatform.dashboard.api;

import com.marinbom2.retailfnbplatform.dashboard.api.dto.DashboardResponse;
import com.marinbom2.retailfnbplatform.dashboard.application.DashboardService;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대시보드", description = "오늘 매장 현황 (SCR-04)")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "오늘 현황", description = "FR-DA-001. 매출·시간대별 매출·인기 상품·안전재고 이하·처리할 이동")
    @GetMapping("/api/v1/stores/{storeId}/dashboard")
    public DashboardResponse today(@PathVariable Long storeId, @AuthenticationPrincipal Member member) {
        return dashboardService.today(storeId, member);
    }
}
