package com.marinbom2.retailfnbplatform.inventory.api;

import com.marinbom2.retailfnbplatform.inventory.api.dto.AdjustStockRequest;
import com.marinbom2.retailfnbplatform.inventory.api.dto.ReceiveStockRequest;
import com.marinbom2.retailfnbplatform.inventory.api.dto.ReorderPointRequest;
import com.marinbom2.retailfnbplatform.inventory.api.dto.StockItemResponse;
import com.marinbom2.retailfnbplatform.inventory.api.dto.StockMovementResponse;
import com.marinbom2.retailfnbplatform.inventory.api.dto.StoreAvailabilityResponse;
import com.marinbom2.retailfnbplatform.inventory.application.InventoryService;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "재고", description = "매장 재고 (SCR-07)")
@RestController
@RequestMapping("/api/v1/stores/{storeId}/stocks")
@RequiredArgsConstructor
public class StockController {

    private final InventoryService inventoryService;

    @Operation(summary = "매장 재고", description = "FR-IN-004. 재고를 세는 SKU 전부와 안전재고")
    @GetMapping
    public List<StockItemResponse> list(@PathVariable Long storeId, @AuthenticationPrincipal Member member) {
        return inventoryService.list(storeId, member);
    }

    @Operation(summary = "입고", description = "FR-IN-001. 점주·매니저")
    @PostMapping("/{skuId}/receive")
    public StockItemResponse receive(@PathVariable Long storeId, @PathVariable Long skuId,
                                     @Valid @RequestBody ReceiveStockRequest request, @AuthenticationPrincipal Member member) {
        return inventoryService.receive(storeId, skuId, request.quantity(), member);
    }

    @Operation(summary = "실사 조정", description = "FR-IN-002. 센 수량으로 맞추고 차이를 원장에 남긴다")
    @PostMapping("/{skuId}/adjust")
    public StockItemResponse adjust(@PathVariable Long storeId, @PathVariable Long skuId,
                                    @Valid @RequestBody AdjustStockRequest request, @AuthenticationPrincipal Member member) {
        return inventoryService.adjust(storeId, skuId, request.countedQuantity(), member);
    }

    @Operation(summary = "안전재고 설정", description = "FR-IN-005. 수량이 이 값 이하면 대시보드에 뜬다. 0 이면 끈다")
    @PutMapping("/{skuId}/reorder-point")
    public StockItemResponse reorderPoint(@PathVariable Long storeId, @PathVariable Long skuId,
                                          @Valid @RequestBody ReorderPointRequest request, @AuthenticationPrincipal Member member) {
        return inventoryService.changeReorderPoint(storeId, skuId, request.reorderPoint(), member);
    }

    @Operation(summary = "다른 매장 재고", description = "FR-IN-006. 같은 브랜드 영업 중 매장 중 재고가 있는 곳, 많은 순")
    @GetMapping("/{skuId}/availability")
    public List<StoreAvailabilityResponse> availability(@PathVariable Long storeId, @PathVariable Long skuId,
                                                        @AuthenticationPrincipal Member member) {
        return inventoryService.availability(storeId, skuId, member);
    }

    @Operation(summary = "재고 변동 이력", description = "FR-IN-004. 최근 50건")
    @GetMapping("/{skuId}/movements")
    public List<StockMovementResponse> movements(@PathVariable Long storeId, @PathVariable Long skuId,
                                                 @AuthenticationPrincipal Member member) {
        return inventoryService.movements(storeId, skuId, member);
    }
}
