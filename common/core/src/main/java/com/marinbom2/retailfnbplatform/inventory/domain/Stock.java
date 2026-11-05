package com.marinbom2.retailfnbplatform.inventory.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 × SKU 의 현재 재고 수량. 수량을 바꾸는 메서드는 모두 같은 변동 기록(StockMovement)을 돌려준다.
 * "수량 변경과 변동 기록은 함께 남는다"는 불변식을 이 클래스 한 곳에서 지킨다. 호출부는 돌려받은 기록을 같은 트랜잭션에서 저장한다.
 * 동시 판매는 @Version 낙관적 락으로 막는다 (md/domain-design.md §7).
 */
@Getter
@Entity
@Table(name = "stock", uniqueConstraints = @UniqueConstraint(name = "uk_stock_store_sku", columnNames = {"store_id", "sku_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private Long skuId;

    /** SKU 단위(StockUnit)의 정수. 0 미만이 되지 않는다 */
    @Column(nullable = false)
    private long quantity;

    @Version
    private long version;

    /** 처음 입고 전의 빈 재고 */
    public static Stock empty(Long storeId, Long skuId) {
        Stock stock = new Stock();
        stock.storeId = Objects.requireNonNull(storeId);
        stock.skuId = Objects.requireNonNull(skuId);
        stock.quantity = 0;
        return stock;
    }

    public StockMovement receive(long amount, StockReference reference) {
        return increase(MovementType.RECEIVE, amount, reference);
    }

    /** @throws InsufficientStockException 남은 수량보다 많이 뺄 때. 수량과 기록은 바뀌지 않는다 */
    public StockMovement deductForSale(long amount, StockReference reference) {
        return decrease(MovementType.SALE, amount, reference);
    }

    public StockMovement restoreForCancel(long amount, StockReference reference) {
        return increase(MovementType.SALE_CANCEL, amount, reference);
    }

    public StockMovement transferOut(long amount, StockReference reference) {
        return decrease(MovementType.TRANSFER_OUT, amount, reference);
    }

    public StockMovement transferIn(long amount, StockReference reference) {
        return increase(MovementType.TRANSFER_IN, amount, reference);
    }

    /** 실사: 센 수량으로 맞추고 차이를 기록한다. 차이가 0 이어도 실사했다는 기록은 남긴다 */
    public StockMovement adjustTo(long countedQuantity, StockReference reference) {
        if (countedQuantity < 0) {
            throw new IllegalArgumentException("실사 수량은 0 이상이어야 합니다");
        }
        long delta = countedQuantity - quantity;
        quantity = countedQuantity;
        return record(MovementType.ADJUST, delta, reference);
    }

    private StockMovement increase(MovementType type, long amount, StockReference reference) {
        requirePositive(amount);
        quantity = Math.addExact(quantity, amount);
        return record(type, amount, reference);
    }

    private StockMovement decrease(MovementType type, long amount, StockReference reference) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new InsufficientStockException(storeId, skuId, quantity, amount);
        }
        quantity -= amount;
        return record(type, -amount, reference);
    }

    private StockMovement record(MovementType type, long delta, StockReference reference) {
        return new StockMovement(this, type, delta, Objects.requireNonNull(reference));
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("변동 수량은 1 이상이어야 합니다: " + amount);
        }
    }
}
