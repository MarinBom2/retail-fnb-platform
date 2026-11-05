package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(@NotBlank String role) {
}
