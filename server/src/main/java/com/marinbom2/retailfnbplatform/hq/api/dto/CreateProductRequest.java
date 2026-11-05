package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 상품 등록. kind 는 ProductKind code. 의류는 색상×사이즈 SKU 를 한 번에 보낸다 */
public record CreateProductRequest(
        @NotBlank String kind,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 50) String category,
        @NotEmpty @Valid List<NewSkuRequest> skus
) {
}
