package com.marinbom2.retailfnbplatform.order.api;

import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.order.api.dto.ChangeQuantityRequest;
import com.marinbom2.retailfnbplatform.order.api.dto.OpenOrderRequest;
import com.marinbom2.retailfnbplatform.order.api.dto.OrderItemRequest;
import com.marinbom2.retailfnbplatform.order.api.dto.OrderResponse;
import com.marinbom2.retailfnbplatform.order.api.dto.PaymentRequest;
import com.marinbom2.retailfnbplatform.order.application.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문", description = "POS 판매와 주문 내역 (SCR-05, SCR-06)")
@RestController
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 시작", description = "FR-OR-001. 계산대(리테일), 매장 식사·포장(외식). 처음 담을 상품을 함께 보낼 수 있다")
    @ApiResponse(responseCode = "409", description = "STORE_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse open(@PathVariable Long storeId, @Valid @RequestBody OpenOrderRequest request,
                              @AuthenticationPrincipal Member member) {
        return orderService.open(storeId, request, member);
    }

    @Operation(summary = "주문 목록", description = "FR-OR-005. 최신 50건. status 로 거른다 (OPEN 이면 이용 중 테이블)")
    @GetMapping
    public List<OrderResponse> list(@PathVariable Long storeId, @RequestParam(required = false) String status,
                                    @AuthenticationPrincipal Member member) {
        return orderService.list(storeId, status, member);
    }

    @Operation(summary = "주문 상세", description = "FR-OR-005")
    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long storeId, @PathVariable Long orderId, @AuthenticationPrincipal Member member) {
        return orderService.get(storeId, orderId, member);
    }

    @Operation(summary = "상품 담기", description = "FR-OR-001. 같은 SKU 면 수량을 더한다")
    @PostMapping("/{orderId}/items")
    public OrderResponse addItem(@PathVariable Long storeId, @PathVariable Long orderId,
                                 @Valid @RequestBody OrderItemRequest request, @AuthenticationPrincipal Member member) {
        return orderService.addItem(storeId, orderId, request, member);
    }

    @Operation(summary = "수량 바꾸기", description = "FR-OR-001")
    @PatchMapping("/{orderId}/items/{skuId}")
    public OrderResponse changeQuantity(@PathVariable Long storeId, @PathVariable Long orderId, @PathVariable Long skuId,
                                        @Valid @RequestBody ChangeQuantityRequest request, @AuthenticationPrincipal Member member) {
        return orderService.changeQuantity(storeId, orderId, skuId, request.quantity(), member);
    }

    @Operation(summary = "상품 빼기", description = "FR-OR-001")
    @DeleteMapping("/{orderId}/items/{skuId}")
    public OrderResponse removeItem(@PathVariable Long storeId, @PathVariable Long orderId, @PathVariable Long skuId,
                                    @AuthenticationPrincipal Member member) {
        return orderService.removeItem(storeId, orderId, skuId, member);
    }

    @Operation(summary = "결제", description = "FR-OR-002. 금액은 합계와 같아야 한다. 재고가 같은 트랜잭션에서 빠진다")
    @ApiResponse(responseCode = "409", description = "OUT_OF_STOCK (skuId 포함), ORDER_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{orderId}/payment")
    public OrderResponse pay(@PathVariable Long storeId, @PathVariable Long orderId,
                             @Valid @RequestBody PaymentRequest request, @AuthenticationPrincipal Member member) {
        return orderService.pay(storeId, orderId, request.method(), request.amount(), member);
    }

    @Operation(summary = "결제 전 취소", description = "FR-OR-003")
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long storeId, @PathVariable Long orderId, @AuthenticationPrincipal Member member) {
        return orderService.cancel(storeId, orderId, member);
    }

    @Operation(summary = "환불", description = "FR-OR-004. 전액. 재고가 돌아온다")
    @PostMapping("/{orderId}/refund")
    public OrderResponse refund(@PathVariable Long storeId, @PathVariable Long orderId, @AuthenticationPrincipal Member member) {
        return orderService.refund(storeId, orderId, member);
    }
}
