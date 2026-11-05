package com.marinbom2.retailfnbplatform.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(@NotNull Long skuId, @Min(1) long quantity) {
}
