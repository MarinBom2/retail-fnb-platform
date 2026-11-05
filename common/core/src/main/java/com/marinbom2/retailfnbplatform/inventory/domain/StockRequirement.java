package com.marinbom2.retailfnbplatform.inventory.domain;

/** 판매 하나로 줄어들 재고: 어느 SKU 를 얼마나 (SKU 단위) */
public record StockRequirement(Long skuId, long quantity) {

    public StockRequirement {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다");
        }
    }
}
