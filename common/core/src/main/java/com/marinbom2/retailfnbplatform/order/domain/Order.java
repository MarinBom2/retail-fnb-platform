package com.marinbom2.retailfnbplatform.order.domain;

import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.order.domain.exception.IllegalOrderStateException;
import com.marinbom2.retailfnbplatform.order.domain.exception.InvalidOrderException;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.exception.IllegalStoreStateException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 (애그리거트 루트). 줄 추가·수량 변경·합계·상태 전이를 이 클래스만 한다.
 * 합계(totalAmount)는 줄이 바뀔 때마다 다시 계산해 저장한다. 결제 금액은 합계와 같아야 한다.
 * 재고 차감은 주문이 하지 않는다. 결제 뒤 OrderPaymentService 가 StockDeductionPolicy 로 뺀다.
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, updatable = false, length = 30)
    private BusinessType businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, updatable = false, length = 20)
    private OrderType orderType;

    /** 매장 식사(DINE_IN)만. 테이블 관리(배치·합석)는 아직 없다 (md/domain-design.md §14) */
    @Column(name = "table_label", updatable = false, length = 20)
    private String tableLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    private long version;

    /**
     * 주문 시작. 영업 중인 매장이어야 하고, 업종이 허용하는 주문 방식이어야 한다.
     *
     * @param tableLabel 매장 식사면 필수, 그 밖에는 null
     */
    public static Order open(Store store, OrderType orderType, String tableLabel) {
        if (!store.isOperating()) {
            throw new IllegalStoreStateException(store.getId(), store.getStatus(), "take order");
        }
        if (!orderType.isAllowedFor(store.getBusinessType())) {
            throw new InvalidOrderException(store.getBusinessType() + " 매장은 " + orderType + " 주문을 받지 않습니다");
        }
        boolean hasTable = tableLabel != null && !tableLabel.isBlank();
        if (orderType.requiresTable() != hasTable) {
            throw new InvalidOrderException(orderType.requiresTable() ? "테이블 번호가 필요합니다" : "테이블 번호는 매장 식사에만 둡니다");
        }
        Order order = new Order();
        order.storeId = Objects.requireNonNull(store.getId(), "저장된 매장이어야 합니다");
        order.brandId = store.getBrandId();
        order.businessType = store.getBusinessType();
        order.orderType = orderType;
        order.tableLabel = hasTable ? tableLabel.trim() : null;
        order.status = OrderStatus.OPEN;
        order.totalAmount = Money.ZERO;
        return order;
    }

    /** 담기. 같은 SKU 를 다시 담으면 (바코드를 두 번 찍으면) 수량을 더한다 */
    public void addItem(Sku sku, long quantity) {
        requireStatus(OrderStatus.OPEN, "add item");
        requirePositive(quantity);
        if (!Objects.equals(sku.getBrandId(), brandId)) {
            throw new InvalidOrderException("다른 브랜드의 상품입니다: " + sku.getSkuCode());
        }
        if (!sku.isSellable()) {
            throw new InvalidOrderException("팔 수 없는 상품입니다: " + sku.getSkuCode());
        }
        findLine(sku.getId()).ifPresentOrElse(
                line -> line.addQuantity(quantity),
                () -> lines.add(new OrderLine(this, Objects.requireNonNull(sku.getId()), sku.displayName(), sku.getPrice(), quantity)));
        recalculate();
    }

    public void changeQuantity(Long skuId, long quantity) {
        requireStatus(OrderStatus.OPEN, "change quantity");
        requirePositive(quantity);
        lineOf(skuId).changeQuantity(quantity);
        recalculate();
    }

    public void removeItem(Long skuId) {
        requireStatus(OrderStatus.OPEN, "remove item");
        lines.remove(lineOf(skuId));
        recalculate();
    }

    /** 결제 완료. 받은 금액이 합계와 같아야 한다 (부분 결제·더치페이는 아직 없음) */
    public void pay(PaymentMethod method, Money paidAmount, Instant paidAt) {
        requireStatus(OrderStatus.OPEN, "pay");
        if (lines.isEmpty()) {
            throw new InvalidOrderException("담긴 상품이 없습니다");
        }
        if (!totalAmount.equals(paidAmount)) {
            throw new InvalidOrderException("결제 금액이 합계와 다릅니다: total=" + totalAmount.amount() + " paid=" + paidAmount.amount());
        }
        this.paymentMethod = Objects.requireNonNull(method);
        this.paidAt = Objects.requireNonNull(paidAt);
        this.status = OrderStatus.PAID;
    }

    /** 결제 전 취소. 재고를 건드리지 않았으므로 되돌릴 것도 없다 */
    public void cancel() {
        requireStatus(OrderStatus.OPEN, "cancel");
        this.status = OrderStatus.CANCELLED;
    }

    /** 결제 후 전액 환불. 호출부가 재고를 되돌린다 */
    public void refund() {
        requireStatus(OrderStatus.PAID, "refund");
        this.status = OrderStatus.REFUNDED;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void recalculate() {
        this.totalAmount = lines.stream().map(OrderLine::lineAmount).reduce(Money.ZERO, Money::plus);
    }

    private OrderLine lineOf(Long skuId) {
        return findLine(skuId).orElseThrow(() -> new InvalidOrderException("주문에 없는 상품입니다: sku " + skuId));
    }

    private Optional<OrderLine> findLine(Long skuId) {
        return lines.stream().filter(line -> line.getSkuId().equals(skuId)).findFirst();
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new IllegalOrderStateException(id, status, action);
        }
    }

    private static void requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new InvalidOrderException("수량은 1 이상이어야 합니다");
        }
    }
}
