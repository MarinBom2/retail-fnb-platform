package com.marinbom2.retailfnbplatform.hq.api.dto;

import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import java.util.List;

public record RecipeResponse(Long menuSkuId, List<Line> lines) {

    public record Line(Long ingredientSkuId, String ingredientName, StockUnit unit, long quantity) {
    }
}
