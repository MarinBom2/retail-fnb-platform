package com.marinbom2.retailfnbplatform.order.domain;

import com.marinbom2.retailfnbplatform.domain.base.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 한 줄. 이름·단가는 담은 순간의 값으로 남긴다 (나중에 가격이 바뀌어도 영수증은 그대로).
 * Order 만 만들고 바꾼다.
 */
@Getter
@Entity
@Table(name = "order_line")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private Long skuId;

    @Column(name = "item_name", nullable = false, updatable = false, length = 250)
    private String itemName;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false, updatable = false))
    private Money unitPrice;

    @Column(nullable = false)
    private long quantity;

    OrderLine(Order order, Long skuId, String itemName, Money unitPrice, long quantity) {
        this.order = order;
        this.skuId = skuId;
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Money lineAmount() {
        return unitPrice.times(quantity);
    }

    void addQuantity(long more) {
        this.quantity = Math.addExact(quantity, more);
    }

    void changeQuantity(long quantity) {
        this.quantity = quantity;
    }
}
