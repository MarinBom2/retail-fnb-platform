package com.marinbom2.retailfnbplatform.hq.api.dto;

import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductKind;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductStatus;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.StockUnit;
import java.util.List;
import java.util.Set;

/** 본사 상품 (md/api.md §8.3) */
public record HqProductResponse(
        Long productId,
        String name,
        String category,
        ProductKind kind,
        ProductStatus status,
        List<HqSku> skus
) {

    public record HqSku(
            Long skuId,
            String skuCode,
            String barcode,
            String displayName,
            String color,
            String size,
            long price,
            StockUnit unit,
            boolean hasRecipe
    ) {

        public static HqSku of(Sku sku, boolean hasRecipe) {
            return new HqSku(sku.getId(), sku.getSkuCode(), sku.getBarcode(), sku.displayName(),
                    sku.getOption() == null ? null : sku.getOption().color(),
                    sku.getOption() == null ? null : sku.getOption().size(),
                    sku.getPrice().amount(), sku.getUnit(), hasRecipe);
        }
    }

    /** @param skusWithRecipe 레시피가 있는 메뉴 SKU ID */
    public static HqProductResponse of(Product product, Set<Long> skusWithRecipe) {
        return new HqProductResponse(product.getId(), product.getName(), product.getCategory(), product.getKind(),
                product.getStatus(),
                product.getSkus().stream().map(sku -> HqSku.of(sku, skusWithRecipe.contains(sku.getId()))).toList());
    }
}
