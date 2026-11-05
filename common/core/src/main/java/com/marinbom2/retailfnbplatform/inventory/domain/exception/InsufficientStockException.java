package com.marinbom2.retailfnbplatform.inventory.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;
import lombok.Getter;

/** 남은 재고보다 많이 빼려 할 때. 화면에는 수량을 내보내지 않고 OUT_OF_STOCK 만 준다 */
@Getter
public class InsufficientStockException extends DomainException {

    private final Long skuId;

    public InsufficientStockException(Long storeId, Long skuId, long available, long requested) {
        super("store " + storeId + " sku " + skuId + " available=" + available + " requested=" + requested);
        this.skuId = skuId;
    }
}
