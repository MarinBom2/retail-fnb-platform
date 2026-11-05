package com.marinbom2.retailfnbplatform.fixture;

import com.marinbom2.retailfnbplatform.catalog.domain.ApparelOption;
import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductKind;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 세 업종의 도메인 테스트 데이터. DB 없이 쓰려고 저장된 것처럼 ID 를 넣는다.
 * 다이소(생활용품)·지오다노(의류)·한식당(외식)을 예로 든다.
 */
public final class DomainFixtures {

    private static final AtomicLong IDS = new AtomicLong(1000);

    private DomainFixtures() {
    }

    public static Brand brand(BusinessType type) {
        String name = switch (type) {
            case GENERAL_MERCHANDISE -> "다이소";
            case APPAREL -> "지오다노";
            case FOOD_SERVICE -> "한식당";
        };
        return withId(Brand.register(name, type));
    }

    /** 영업 중인 매장 */
    public static Store openStore(Brand brand) {
        Store store = withId(Store.create(brand, "S-" + IDS.incrementAndGet(), brand.getName() + " 강남점"));
        store.open();
        return store;
    }

    /** 생활용품: 단일 SKU, 바코드 */
    public static Sku merchandiseSku(Brand brand, String name, long price) {
        Product product = withId(Product.create(brand, ProductKind.MERCHANDISE, name, "생활용품"));
        return withId(product.addSku("GM-" + IDS.incrementAndGet(), "880" + IDS.get(), Money.won(price), null, StockUnit.EA));
    }

    /** 의류: 같은 스타일의 색상·사이즈 SKU */
    public static Sku apparelSku(Product style, String color, String size, long price) {
        return withId(style.addSku(style.getName() + "-" + color + "-" + size, null, Money.won(price),
                new ApparelOption(color, size), StockUnit.EA));
    }

    public static Product apparelStyle(Brand brand, String name) {
        return withId(Product.create(brand, ProductKind.MERCHANDISE, name, "티셔츠"));
    }

    public static Sku menuSku(Brand brand, String name, long price) {
        Product product = withId(Product.create(brand, ProductKind.MENU, name, "식사"));
        return withId(product.addSku("MN-" + IDS.incrementAndGet(), null, Money.won(price), null, StockUnit.EA));
    }

    public static Sku ingredientSku(Brand brand, String name, StockUnit unit) {
        Product product = withId(Product.create(brand, ProductKind.INGREDIENT, name, "식자재"));
        return withId(product.addSku("IG-" + IDS.incrementAndGet(), null, null, null, unit));
    }

    public static Stock stock(Long storeId, Long skuId) {
        return withId(Stock.empty(storeId, skuId));
    }

    public static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", IDS.incrementAndGet());
        return entity;
    }
}
