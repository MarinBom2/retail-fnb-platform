package com.marinbom2.retailfnbplatform.inventory.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    /** 이 매장이 보내거나 받는 이동, 최신 순 (두 인자에 같은 매장 ID) */
    List<StockTransfer> findTop50ByFromStoreIdOrToStoreIdOrderByIdDesc(Long fromStoreId, Long toStoreId);

    long countByFromStoreIdAndStatus(Long fromStoreId, TransferStatus status);

    long countByToStoreIdAndStatus(Long toStoreId, TransferStatus status);
}
