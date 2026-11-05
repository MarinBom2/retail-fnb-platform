package com.marinbom2.retailfnbplatform.catalog.api.dto;

import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductKind;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductStatus;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import java.util.List;
import java.util.Map;

/** 브랜드 상품과 이 매장 재고 (md/api.md §3) */
public record CatalogProductResponse(
        Long productId,
        String name,
        String category,
        ProductKind kind,
        ProductStatus status,
        List<SkuItem> skus
) {

    public record SkuItem(
            Long skuId,
            String skuCode,
            String barcode,
            String displayName,
            String color,
            String size,
            long price,
            StockUnit unit,
            boolean sellable,
            boolean stockTracked,
            Long stockQuantity
    ) {
    }

    /** @param quantities 이 매장 SKU 별 수량. 없으면 0 */
    public static CatalogProductResponse of(Product product, Map<Long, Long> quantities) {
        return new CatalogProductResponse(product.getId(), product.getName(), product.getCategory(), product.getKind(),
                product.getStatus(), product.getSkus().stream().map(sku -> sku(sku, quantities)).toList());
    }

    private static SkuItem sku(Sku sku, Map<Long, Long> quantities) {
        boolean tracked = sku.tracksStock();
        return new SkuItem(sku.getId(), sku.getSkuCode(), sku.getBarcode(), sku.displayName(),
                sku.getOption() == null ? null : sku.getOption().color(),
                sku.getOption() == null ? null : sku.getOption().size(),
                sku.getPrice().amount(), sku.getUnit(), sku.isSellable(), tracked,
                tracked ? quantities.getOrDefault(sku.getId(), 0L) : null);
    }
}
