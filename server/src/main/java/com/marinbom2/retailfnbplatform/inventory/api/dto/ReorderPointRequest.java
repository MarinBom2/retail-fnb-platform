package com.marinbom2.retailfnbplatform.inventory.api.dto;

import jakarta.validation.constraints.Min;

public record ReorderPointRequest(@Min(0) long reorderPoint) {
}
