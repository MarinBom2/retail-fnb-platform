package com.marinbom2.retailfnbplatform.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.catalog.domain.exception.InvalidCatalogException;
import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecipeTest {

    private final Brand restaurant = DomainFixtures.brand(BusinessType.FOOD_SERVICE);
    private final Sku bibimbap = DomainFixtures.menuSku(restaurant, "비빔밥", 9_000);
    private final Sku rice = DomainFixtures.ingredientSku(restaurant, "쌀", StockUnit.GRAM);
    private final Sku egg = DomainFixtures.ingredientSku(restaurant, "달걀", StockUnit.EA);

    @Test
    @DisplayName("메뉴 하나에 식자재와 양")
    void define() {
        Recipe recipe = Recipe.define(bibimbap, List.of(RecipeLine.of(rice, 200), RecipeLine.of(egg, 1)));

        assertThat(recipe.getMenuSkuId()).isEqualTo(bibimbap.getId());
        assertThat(recipe.getLines()).containsExactly(new RecipeLine(rice.getId(), 200), new RecipeLine(egg.getId(), 1));
    }

    @Test
    @DisplayName("메뉴가 아닌 SKU, 식자재가 아닌 재료, 0 이하 양, 빈 레시피, 같은 식자재 두 번은 안 된다")
    void rules() {
        assertThatThrownBy(() -> Recipe.define(rice, List.of(RecipeLine.of(egg, 1)))).isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> RecipeLine.of(bibimbap, 1)).isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> RecipeLine.of(rice, 0)).isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> Recipe.define(bibimbap, List.of())).isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> Recipe.define(bibimbap, List.of(RecipeLine.of(rice, 100), RecipeLine.of(rice, 100))))
                .isInstanceOf(InvalidCatalogException.class);
    }
}
