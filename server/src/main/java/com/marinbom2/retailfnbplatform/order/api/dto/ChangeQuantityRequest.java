package com.marinbom2.retailfnbplatform.order.api.dto;

import jakarta.validation.constraints.Min;

public record ChangeQuantityRequest(@Min(1) long quantity) {
}
