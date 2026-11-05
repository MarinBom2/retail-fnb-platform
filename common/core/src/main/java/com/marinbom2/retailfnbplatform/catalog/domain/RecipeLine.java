package com.marinbom2.retailfnbplatform.catalog.domain;

import com.marinbom2.retailfnbplatform.catalog.domain.exception.InvalidCatalogException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** 메뉴 1개에 드는 식자재 하나와 그 양 (식자재 SKU 단위 기준) */
@Embeddable
public record RecipeLine(
        @Column(name = "ingredient_sku_id", nullable = false) Long ingredientSkuId,
        @Column(nullable = false) long quantity
) {

    public static RecipeLine of(Sku ingredient, long quantity) {
        if (ingredient.getKind() != ProductKind.INGREDIENT) {
            throw new InvalidCatalogException("레시피에는 식자재 SKU 만 넣을 수 있습니다: " + ingredient.getSkuCode());
        }
        if (quantity <= 0) {
            throw new InvalidCatalogException("식자재 양은 1 이상이어야 합니다");
        }
        return new RecipeLine(Objects.requireNonNull(ingredient.getId(), "저장된 식자재여야 합니다"), quantity);
    }
}
