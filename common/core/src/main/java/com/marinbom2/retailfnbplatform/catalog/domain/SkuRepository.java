package com.marinbom2.retailfnbplatform.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    /** 상품 등록: SKU 코드·바코드는 브랜드 안에서 하나 */
    boolean existsByBrandIdAndSkuCode(Long brandId, String skuCode);

    boolean existsByBrandIdAndBarcode(Long brandId, String barcode);
}
