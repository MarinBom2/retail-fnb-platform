package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.Min;

public record PriceRequest(@Min(0) long price) {
}
