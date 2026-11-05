package com.marinbom2.retailfnbplatform.inventory.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.catalog.domain.Recipe;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeLine;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRequirement;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StockDeductionPolicyTest {

    @Test
    @DisplayName("리테일: 판 SKU 를 판 수량만큼")
    void direct() {
        DirectSkuDeduction policy = new DirectSkuDeduction(BusinessType.APPAREL);

        assertThat(policy.requirementsFor(7L, 3)).containsExactly(new StockRequirement(7L, 3));
    }

    @Test
    @DisplayName("외식: 레시피 양 × 판매 수량만큼 식자재를, 레시피가 없으면 빈 목록 (예외 아님)")
    void recipe() {
        Brand brand = DomainFixtures.brand(BusinessType.FOOD_SERVICE);
        Sku menu = DomainFixtures.menuSku(brand, "비빔밥", 9_000);
        Sku rice = DomainFixtures.ingredientSku(brand, "쌀", StockUnit.GRAM);
        Sku egg = DomainFixtures.ingredientSku(brand, "달걀", StockUnit.EA);
        RecipeRepository recipes = Mockito.mock(RecipeRepository.class);
        Mockito.when(recipes.findByMenuSkuId(menu.getId()))
                .thenReturn(Optional.of(Recipe.define(menu, List.of(RecipeLine.of(rice, 200), RecipeLine.of(egg, 1)))));
        RecipeStockDeduction policy = new RecipeStockDeduction(recipes);

        assertThat(policy.requirementsFor(menu.getId(), 2))
                .containsExactly(new StockRequirement(rice.getId(), 400), new StockRequirement(egg.getId(), 2));
        assertThat(policy.requirementsFor(999L, 1)).isEmpty();
    }

    @Test
    @DisplayName("모든 업종에 정책이 정확히 하나: 빠지거나 겹치면 기동 실패")
    void oneBeanPerBusinessType() {
        InventoryPolicyConfig config = new InventoryPolicyConfig();
        RecipeRepository recipes = Mockito.mock(RecipeRepository.class);
        List<com.marinbom2.retailfnbplatform.inventory.domain.StockDeductionPolicy> all = List.of(
                config.generalMerchandiseDeduction(), config.apparelDeduction(), config.foodServiceDeduction(recipes));

        StockDeductionPolicies policies = config.stockDeductionPolicies(all);
        assertThat(policies.of(BusinessType.FOOD_SERVICE)).isInstanceOf(RecipeStockDeduction.class);
        assertThat(policies.of(BusinessType.GENERAL_MERCHANDISE)).isInstanceOf(DirectSkuDeduction.class);

        assertThatThrownBy(() -> new StockDeductionPolicies(all.subList(0, 2))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new StockDeductionPolicies(List.of(all.get(0), all.get(0), all.get(1), all.get(2))))
                .isInstanceOf(IllegalStateException.class);
    }
}
