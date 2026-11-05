package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 새 SKU (md/api.md §8.3). color·size 는 의류만, price 는 식자재면 무시한다. unit 은 StockUnit code
 */
public record NewSkuRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "SKU 코드는 영문·숫자·하이픈·밑줄만 씁니다.") String skuCode,
        @Size(max = 50) String barcode,
        @Min(0) Long price,
        @Size(max = 30) String color,
        @Size(max = 10) String size,
        @NotBlank String unit
) {
}
