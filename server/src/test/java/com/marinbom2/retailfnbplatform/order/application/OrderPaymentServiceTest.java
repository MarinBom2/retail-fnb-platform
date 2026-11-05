package com.marinbom2.retailfnbplatform.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marinbom2.retailfnbplatform.catalog.domain.Recipe;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeLine;
import com.marinbom2.retailfnbplatform.catalog.domain.RecipeRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.fixture.DomainFixtures;
import com.marinbom2.retailfnbplatform.inventory.domain.MovementType;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovement;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovementRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockReference;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InsufficientStockException;
import com.marinbom2.retailfnbplatform.inventory.policy.InventoryPolicyConfig;
import com.marinbom2.retailfnbplatform.order.domain.Order;
import com.marinbom2.retailfnbplatform.order.domain.OrderRepository;
import com.marinbom2.retailfnbplatform.order.domain.OrderStatus;
import com.marinbom2.retailfnbplatform.order.domain.OrderType;
import com.marinbom2.retailfnbplatform.order.domain.PaymentMethod;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 결제 → 업종별 재고 차감, 환불 → 복원 (md/domain-design.md §6). 리포지토리는 메모리 대역 */
class OrderPaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");

    private final OrderRepository orders = Mockito.mock(OrderRepository.class);
    private final StockRepository stocks = Mockito.mock(StockRepository.class);
    private final StockMovementRepository movements = Mockito.mock(StockMovementRepository.class);
    private final RecipeRepository recipes = Mockito.mock(RecipeRepository.class);
    private final Map<Long, Stock> stockBySku = new HashMap<>();
    private final List<StockMovement> saved = new ArrayList<>();

    private OrderPaymentService service;

    @BeforeEach
    void setUp() {
        InventoryPolicyConfig config = new InventoryPolicyConfig();
        service = new OrderPaymentService(orders, stocks, movements,
                config.stockDeductionPolicies(List.of(config.generalMerchandiseDeduction(), config.apparelDeduction(),
                        config.foodServiceDeduction(recipes))),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(stocks.findByStoreIdAndSkuId(any(), any()))
                .thenAnswer(inv -> Optional.ofNullable(stockBySku.get(inv.<Long>getArgument(1))));
        when(movements.save(any())).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    @Test
    @DisplayName("다이소: 결제하면 판 SKU 재고가 빠지고 주문 번호가 출처로 남는다. 환불하면 돌아온다")
    void retailPayAndRefund() {
        Brand daiso = DomainFixtures.brand(BusinessType.GENERAL_MERCHANDISE);
        Store store = DomainFixtures.openStore(daiso);
        Sku cup = DomainFixtures.merchandiseSku(daiso, "머그컵", 2_000);
        Stock cupStock = receive(store, cup, 10);
        Order order = saveOrder(Order.open(store, OrderType.COUNTER, null));
        order.addItem(cup, 3);

        service.pay(order.getId(), PaymentMethod.CASH, Money.won(6_000));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(NOW);
        assertThat(cupStock.getQuantity()).isEqualTo(7);
        assertThat(saved).singleElement().satisfies(m -> {
            assertThat(m.getType()).isEqualTo(MovementType.SALE);
            assertThat(m.getReference()).isEqualTo(StockReference.order(order.getId()));
        });

        service.refund(order.getId());
        assertThat(cupStock.getQuantity()).isEqualTo(10);
        assertThat(saved.getLast().getType()).isEqualTo(MovementType.SALE_CANCEL);
    }

    @Test
    @DisplayName("음식점: 메뉴가 아니라 레시피의 식자재가 빠지고, 두 메뉴가 같은 식자재를 쓰면 합쳐서 한 번에 뺀다")
    void foodServiceRecipe() {
        Brand brand = DomainFixtures.brand(BusinessType.FOOD_SERVICE);
        Store store = DomainFixtures.openStore(brand);
        Sku bibimbap = DomainFixtures.menuSku(brand, "비빔밥", 9_000);
        Sku omurice = DomainFixtures.menuSku(brand, "오므라이스", 10_000);
        Sku rice = DomainFixtures.ingredientSku(brand, "쌀", StockUnit.GRAM);
        Sku egg = DomainFixtures.ingredientSku(brand, "달걀", StockUnit.EA);
        Stock riceStock = receive(store, rice, 1_000);
        Stock eggStock = receive(store, egg, 10);
        when(recipes.findByMenuSkuId(bibimbap.getId()))
                .thenReturn(Optional.of(Recipe.define(bibimbap, List.of(RecipeLine.of(rice, 200), RecipeLine.of(egg, 1)))));
        when(recipes.findByMenuSkuId(omurice.getId()))
                .thenReturn(Optional.of(Recipe.define(omurice, List.of(RecipeLine.of(rice, 150), RecipeLine.of(egg, 2)))));
        Order order = saveOrder(Order.open(store, OrderType.DINE_IN, "5"));
        order.addItem(bibimbap, 2);
        order.addItem(omurice, 1);

        service.pay(order.getId(), PaymentMethod.CARD, Money.won(28_000));

        assertThat(riceStock.getQuantity()).isEqualTo(1_000 - 200 * 2 - 150);
        assertThat(eggStock.getQuantity()).isEqualTo(10 - 2 - 2);
        assertThat(saved).hasSize(2);
    }

    @Test
    @DisplayName("재고가 모자라면 예외. 재고 기록은 남지 않는다 (트랜잭션 롤백으로 주문도 OPEN 으로 돌아간다)")
    void outOfStock() {
        Brand giordano = DomainFixtures.brand(BusinessType.APPAREL);
        Store store = DomainFixtures.openStore(giordano);
        Sku blackL = DomainFixtures.apparelSku(DomainFixtures.apparelStyle(giordano, "티셔츠"), "BLACK", "L", 19_900);
        Sku whiteM = DomainFixtures.apparelSku(DomainFixtures.apparelStyle(giordano, "셔츠"), "WHITE", "M", 29_900);
        receive(store, blackL, 1);
        Order order = saveOrder(Order.open(store, OrderType.COUNTER, null));
        order.addItem(blackL, 1);
        order.addItem(whiteM, 1); // 이 매장에 입고된 적이 없다

        assertThatThrownBy(() -> service.pay(order.getId(), PaymentMethod.CARD, Money.won(49_800)))
                .isInstanceOf(InsufficientStockException.class);
        verify(movements, never()).save(Mockito.argThat(m -> m.getSkuId().equals(whiteM.getId())));
    }

    private Stock receive(Store store, Sku sku, long quantity) {
        Stock stock = DomainFixtures.stock(store.getId(), sku.getId());
        stock.receive(quantity, StockReference.manual());
        stockBySku.put(sku.getId(), stock);
        return stock;
    }

    private Order saveOrder(Order order) {
        DomainFixtures.withId(order);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        return order;
    }
}
