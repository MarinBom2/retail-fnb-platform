package com.marinbom2.retailfnbplatform.inventory.policy;

import com.marinbom2.retailfnbplatform.catalog.domain.RecipeRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockDeductionPolicy;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRequirement;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** 외식: 메뉴의 레시피대로 식자재를 판매 수량 배만큼 뺀다. 레시피가 없는 메뉴는 아무것도 빼지 않는다 */
@RequiredArgsConstructor
public class RecipeStockDeduction implements StockDeductionPolicy {

    private final RecipeRepository recipeRepository;

    @Override
    public BusinessType businessType() {
        return BusinessType.FOOD_SERVICE;
    }

    @Override
    public List<StockRequirement> requirementsFor(Long soldSkuId, long soldQuantity) {
        return recipeRepository.findByMenuSkuId(soldSkuId)
                .map(recipe -> recipe.getLines().stream()
                        .map(line -> new StockRequirement(line.ingredientSkuId(), Math.multiplyExact(line.quantity(), soldQuantity)))
                        .toList())
                .orElse(List.of());
    }
}
