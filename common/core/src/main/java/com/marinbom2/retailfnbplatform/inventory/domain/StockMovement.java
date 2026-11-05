package com.marinbom2.retailfnbplatform.inventory.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 변동 기록 (원장). 추가만 하고 고치거나 지우지 않는다. Stock 만 만든다.
 * balanceAfter 를 같이 남겨 기록만으로 어느 시점의 재고를 다시 셀 수 있다.
 */
@Getter
@Entity
@Table(name = "stock_movement", indexes = @Index(name = "ix_stock_movement_stock", columnList = "stock_id, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false, updatable = false)
    private Long stockId;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private Long skuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private MovementType type;

    /** 늘었으면 양수, 줄었으면 음수 */
    @Column(nullable = false, updatable = false)
    private long delta;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private long balanceAfter;

    @Embedded
    private StockReference reference;

    StockMovement(Stock stock, MovementType type, long delta, StockReference reference) {
        this.stockId = java.util.Objects.requireNonNull(stock.getId(), "저장된 재고여야 합니다 (Stock 을 먼저 저장한다)");
        this.storeId = stock.getStoreId();
        this.skuId = stock.getSkuId();
        this.type = type;
        this.delta = delta;
        this.balanceAfter = stock.getQuantity();
        this.reference = reference;
    }
}
