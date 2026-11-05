package com.marinbom2.retailfnbplatform.inventory.api.dto;

import jakarta.validation.constraints.Min;

public record ReceiveStockRequest(@Min(1) long quantity) {
}
