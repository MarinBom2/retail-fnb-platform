package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** businessType 은 BusinessType code (GENERAL_MERCHANDISE, APPAREL, FOOD_SERVICE) */
public record CreateBrandRequest(@NotBlank @Size(max = 100) String name, @NotBlank String businessType) {
}
