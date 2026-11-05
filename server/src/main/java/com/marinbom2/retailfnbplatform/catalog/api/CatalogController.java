package com.marinbom2.retailfnbplatform.catalog.api;

import com.marinbom2.retailfnbplatform.catalog.api.dto.CatalogProductResponse;
import com.marinbom2.retailfnbplatform.catalog.application.CatalogQueryService;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상품", description = "브랜드 카탈로그 (SCR-05, SCR-09)")
@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogQueryService catalogQueryService;

    @Operation(summary = "카탈로그", description = "FR-CA-001. 브랜드 상품·SKU 와 이 매장 재고")
    @GetMapping("/api/v1/stores/{storeId}/catalog")
    public List<CatalogProductResponse> catalog(@PathVariable Long storeId, @AuthenticationPrincipal Member member) {
        return catalogQueryService.catalog(storeId, member);
    }
}
