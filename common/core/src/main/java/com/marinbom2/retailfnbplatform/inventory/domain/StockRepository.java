package com.marinbom2.retailfnbplatform.inventory.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByStoreIdAndSkuId(Long storeId, Long skuId);
}
