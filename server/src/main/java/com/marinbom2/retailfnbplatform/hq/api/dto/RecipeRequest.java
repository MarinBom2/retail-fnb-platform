package com.marinbom2.retailfnbplatform.hq.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 레시피 통째로 바꾸기 (md/api.md §8.3) */
public record RecipeRequest(@NotEmpty @Valid List<Line> lines) {

    public record Line(@NotNull Long ingredientSkuId, @Min(1) long quantity) {
    }
}
