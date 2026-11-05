package com.marinbom2.retailfnbplatform.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 주문 시작. orderType 은 OrderType code (COUNTER, DINE_IN, TAKEOUT). items 는 비어 있어도 된다 */
public record OpenOrderRequest(
        @NotBlank String orderType,
        @Size(max = 20) String tableLabel,
        @NotNull @Valid List<OrderItemRequest> items
) {
}
