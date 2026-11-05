package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 브랜드·매장 이름 바꾸기 */
public record NameRequest(@NotBlank @Size(max = 100) String name) {
}
