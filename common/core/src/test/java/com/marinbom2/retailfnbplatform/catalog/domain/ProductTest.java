package com.marinbom2.retailfnbplatform.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.catalog.domain.exception.InvalidCatalogException;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    private final Brand daiso = DomainFixtures.brand(BusinessType.GENERAL_MERCHANDISE);
    private final Brand giordano = DomainFixtures.brand(BusinessType.APPAREL);
    private final Brand restaurant = DomainFixtures.brand(BusinessType.FOOD_SERVICE);

    @Test
    @DisplayName("업종이 허용하는 상품 종류만: 리테일은 판매 상품, 외식은 메뉴·식자재")
    void kindByBusinessType() {
        assertThatThrownBy(() -> Product.create(daiso, ProductKind.MENU, "김밥", null))
                .isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> Product.create(restaurant, ProductKind.MERCHANDISE, "컵", null))
                .isInstanceOf(InvalidCatalogException.class);
        assertThat(Product.create(restaurant, ProductKind.INGREDIENT, "쌀", null).getKind()).isEqualTo(ProductKind.INGREDIENT);
    }

    @Test
    @DisplayName("의류: 색상×사이즈 SKU 를 여러 개 두고, 같은 조합(대소문자·공백 무시)은 한 번만")
    void apparelVariants() {
        Product tee = DomainFixtures.apparelStyle(giordano, "크루넥 티셔츠");

        tee.addSku("TEE-BK-M", null, Money.won(19_900), new ApparelOption("black", "m"), StockUnit.EA);
        tee.addSku("TEE-BK-L", null, Money.won(19_900), new ApparelOption("BLACK", "L"), StockUnit.EA);

        assertThat(tee.getSkus()).hasSize(2);
        assertThat(tee.getSkus().getFirst().displayName()).isEqualTo("크루넥 티셔츠 (BLACK / M)");
        assertThatThrownBy(() -> tee.addSku("TEE-BK-M2", null, Money.won(19_900), new ApparelOption(" Black ", "M"), StockUnit.EA))
                .isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> tee.addSku("TEE-NONE", null, Money.won(19_900), null, StockUnit.EA))
                .isInstanceOf(InvalidCatalogException.class);
    }

    @Test
    @DisplayName("생활용품·외식: 옵션 없는 SKU 하나만")
    void singleSku() {
        Product cup = Product.create(daiso, ProductKind.MERCHANDISE, "머그컵", "주방");
        cup.addSku("CUP-1", "8801234567890", Money.won(2_000), null, StockUnit.EA);

        assertThatThrownBy(() -> cup.addSku("CUP-2", null, Money.won(2_000), null, StockUnit.EA))
                .isInstanceOf(InvalidCatalogException.class);
        Product other = Product.create(daiso, ProductKind.MERCHANDISE, "접시", "주방");
        assertThatThrownBy(() -> other.addSku("PLATE-1", null, Money.won(2_000), new ApparelOption("W", "S"), StockUnit.EA))
                .isInstanceOf(InvalidCatalogException.class);
    }

    @Test
    @DisplayName("식자재는 팔지 않는다: 가격 0, 가격 변경 불가, 주문에 담을 수 없음")
    void ingredientNotSellable() {
        Sku rice = DomainFixtures.ingredientSku(restaurant, "쌀", StockUnit.GRAM);

        assertThat(rice.getPrice()).isEqualTo(Money.ZERO);
        assertThat(rice.isSellable()).isFalse();
        assertThatThrownBy(() -> rice.changePrice(Money.won(100))).isInstanceOf(InvalidCatalogException.class);
    }

    @Test
    @DisplayName("판매 중지하면 팔 수 없고, 단종은 되돌리거나 SKU 를 더할 수 없다")
    void status() {
        Sku cup = DomainFixtures.merchandiseSku(daiso, "머그컵", 2_000);
        Product product = cup.getProduct();

        product.suspend();
        assertThat(cup.isSellable()).isFalse();
        product.resume();
        assertThat(cup.isSellable()).isTrue();
        product.discontinue();
        assertThatThrownBy(product::resume).isInstanceOf(InvalidCatalogException.class);
        assertThatThrownBy(() -> product.addSku("X", null, Money.won(1), null, StockUnit.EA))
                .isInstanceOf(InvalidCatalogException.class);
    }
}
