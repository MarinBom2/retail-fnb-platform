package com.marinbom2.retailfnbplatform.inventory.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByStoreIdAndSkuId(Long storeId, Long skuId);

    /** 매장 재고 화면, 대시보드 안전재고 */
    List<Stock> findByStoreId(Long storeId);

    /** 다른 매장 재고 찾기 */
    List<Stock> findBySkuId(Long skuId);
}
