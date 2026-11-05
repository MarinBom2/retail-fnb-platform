package com.marinbom2.retailfnbplatform.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.order.domain.exception.IllegalOrderStateException;
import com.marinbom2.retailfnbplatform.order.domain.exception.InvalidOrderException;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.exception.IllegalStoreStateException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");

    private final Brand daiso = DomainFixtures.brand(BusinessType.GENERAL_MERCHANDISE);
    private final Brand giordano = DomainFixtures.brand(BusinessType.APPAREL);
    private final Brand restaurant = DomainFixtures.brand(BusinessType.FOOD_SERVICE);

    @Test
    @DisplayName("다이소: 계산대 주문, 같은 바코드를 두 번 찍으면 한 줄로 합치고 합계를 다시 계산한다")
    void retailCounter() {
        Store store = DomainFixtures.openStore(daiso);
        Sku cup = DomainFixtures.merchandiseSku(daiso, "머그컵", 2_000);
        Sku towel = DomainFixtures.merchandiseSku(daiso, "수건", 3_000);

        Order order = Order.open(store, OrderType.COUNTER, null);
        order.addItem(cup, 1);
        order.addItem(cup, 1);
        order.addItem(towel, 1);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualTo(Money.won(7_000));

        order.changeQuantity(towel.getId(), 3);
        order.removeItem(cup.getId());
        assertThat(order.getTotalAmount()).isEqualTo(Money.won(9_000));
    }

    @Test
    @DisplayName("지오다노: 옵션까지 붙은 이름과 담은 순간의 단가가 남는다 (나중에 가격이 바뀌어도 그대로)")
    void apparelSnapshot() {
        Store store = DomainFixtures.openStore(giordano);
        Product tee = DomainFixtures.apparelStyle(giordano, "크루넥 티셔츠");
        Sku blackL = DomainFixtures.apparelSku(tee, "BLACK", "L", 19_900);

        Order order = Order.open(store, OrderType.COUNTER, null);
        order.addItem(blackL, 2);
        blackL.changePrice(Money.won(9_900));

        assertThat(order.getLines().getFirst().getItemName()).isEqualTo("크루넥 티셔츠 (BLACK / L)");
        assertThat(order.getLines().getFirst().getUnitPrice()).isEqualTo(Money.won(19_900));
        assertThat(order.getTotalAmount()).isEqualTo(Money.won(39_800));
    }

    @Test
    @DisplayName("업종별 주문 방식: 리테일은 계산대만, 외식은 매장 식사(테이블 필수)·포장(테이블 없음)")
    void orderTypeByBusinessType() {
        Store daisoStore = DomainFixtures.openStore(daiso);
        Store restaurantStore = DomainFixtures.openStore(restaurant);

        assertThatThrownBy(() -> Order.open(daisoStore, OrderType.DINE_IN, "3")).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.open(restaurantStore, OrderType.COUNTER, null)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.open(restaurantStore, OrderType.DINE_IN, " ")).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> Order.open(restaurantStore, OrderType.TAKEOUT, "3")).isInstanceOf(InvalidOrderException.class);
        assertThat(Order.open(restaurantStore, OrderType.DINE_IN, " 3 ").getTableLabel()).isEqualTo("3");
    }

    @Test
    @DisplayName("영업 중이 아닌 매장은 주문을 받지 않는다")
    void closedStore() {
        Store store = DomainFixtures.openStore(daiso);
        store.close();

        assertThatThrownBy(() -> Order.open(store, OrderType.COUNTER, null)).isInstanceOf(IllegalStoreStateException.class);
    }

    @Test
    @DisplayName("담을 수 없는 것: 다른 브랜드 상품, 식자재, 판매 중지 상품, 0 개")
    void itemRules() {
        Store store = DomainFixtures.openStore(restaurant);
        Order order = Order.open(store, OrderType.TAKEOUT, null);
        Sku rice = DomainFixtures.ingredientSku(restaurant, "쌀", StockUnit.GRAM);
        Sku menu = DomainFixtures.menuSku(restaurant, "김밥", 4_000);
        Sku daisoCup = DomainFixtures.merchandiseSku(daiso, "머그컵", 2_000);

        assertThatThrownBy(() -> order.addItem(daisoCup, 1)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> order.addItem(rice, 1)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> order.addItem(menu, 0)).isInstanceOf(InvalidOrderException.class);
        menu.getProduct().suspend();
        assertThatThrownBy(() -> order.addItem(menu, 1)).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("결제: 빈 주문·금액 불일치는 거부, 결제 뒤에는 담기·취소·재결제 불가, 환불만 된다")
    void payAndRefund() {
        Store store = DomainFixtures.openStore(restaurant);
        Sku menu = DomainFixtures.menuSku(restaurant, "비빔밥", 9_000);
        Order order = Order.open(store, OrderType.DINE_IN, "5");

        assertThatThrownBy(() -> order.pay(PaymentMethod.CARD, Money.ZERO, NOW)).isInstanceOf(InvalidOrderException.class);
        order.addItem(menu, 2);
        assertThatThrownBy(() -> order.pay(PaymentMethod.CARD, Money.won(17_999), NOW)).isInstanceOf(InvalidOrderException.class);

        order.pay(PaymentMethod.CARD, Money.won(18_000), NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> order.addItem(menu, 1)).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(order::cancel).isInstanceOf(IllegalOrderStateException.class);
        assertThatThrownBy(() -> order.pay(PaymentMethod.CARD, Money.won(18_000), NOW)).isInstanceOf(IllegalOrderStateException.class);

        order.refund();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThatThrownBy(order::refund).isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    @DisplayName("결제 전 취소, 취소한 주문은 환불할 수 없다")
    void cancel() {
        Order order = Order.open(DomainFixtures.openStore(daiso), OrderType.COUNTER, null);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(order::refund).isInstanceOf(IllegalOrderStateException.class);
    }
}
