package com.marinbom2.retailfnbplatform.inventory.api.dto;

import jakarta.validation.constraints.Min;

public record AdjustStockRequest(@Min(0) long countedQuantity) {
}
