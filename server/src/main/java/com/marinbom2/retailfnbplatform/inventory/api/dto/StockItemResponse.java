package com.marinbom2.retailfnbplatform.inventory.api.dto;

import com.marinbom2.retailfnbplatform.catalog.domain.ProductKind;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;

/** 매장 재고 한 줄 (md/api.md §4). 입고 전이면 수량 0, 안전재고 0 */
public record StockItemResponse(
        Long skuId,
        String productName,
        String itemName,
        String skuCode,
        ProductKind kind,
        StockUnit unit,
        long quantity,
        long reorderPoint,
        boolean needsReorder
) {

    /** @param stock 아직 재고 행이 없으면 null */
    public static StockItemResponse of(Sku sku, Stock stock) {
        return new StockItemResponse(sku.getId(), sku.getProduct().getName(), sku.displayName(), sku.getSkuCode(),
                sku.getKind(), sku.getUnit(),
                stock == null ? 0 : stock.getQuantity(),
                stock == null ? 0 : stock.getReorderPoint(),
                stock != null && stock.needsReorder());
    }
}
