package com.marinbom2.retailfnbplatform.inventory.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findTop50ByStockIdOrderByIdDesc(Long stockId);
}
