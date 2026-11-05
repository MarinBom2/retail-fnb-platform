package com.marinbom2.retailfnbplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

/**
 * 재고 변동의 출처 (예: 주문 12번, 이동 3번). 출처가 없는 수동 입고·실사는 {@link #manual()}.
 *
 * @param id 출처 ID. MANUAL 이면 null
 */
@Embeddable
public record StockReference(
        @Enumerated(EnumType.STRING) @Column(name = "reference_type", nullable = false, length = 20) StockReferenceType type,
        @Column(name = "reference_id") Long id
) {

    public StockReference {
        Objects.requireNonNull(type);
    }

    public static StockReference order(Long orderId) {
        return new StockReference(StockReferenceType.ORDER, Objects.requireNonNull(orderId));
    }

    public static StockReference transfer(Long transferId) {
        return new StockReference(StockReferenceType.TRANSFER, Objects.requireNonNull(transferId));
    }

    public static StockReference manual() {
        return new StockReference(StockReferenceType.MANUAL, null);
    }
}
