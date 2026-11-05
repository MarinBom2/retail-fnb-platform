package com.marinbom2.retailfnbplatform.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTransferRequest(@NotBlank @Size(max = 200) String reason) {
}
