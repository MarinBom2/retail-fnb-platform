package com.marinbom2.retailfnbplatform.inventory.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 이동 요청. 경로의 매장이 받는 매장이다 (md/api.md §5) */
public record TransferRequest(
        @NotNull Long fromStoreId,
        @NotEmpty @Valid List<Line> lines,
        @Size(max = 200) String memo
) {

    public record Line(@NotNull Long skuId, @Min(1) long quantity) {
    }
}
