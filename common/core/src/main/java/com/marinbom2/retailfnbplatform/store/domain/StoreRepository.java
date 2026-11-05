package com.marinbom2.retailfnbplatform.store.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    /** 본사 매장 목록, 브랜드 관리자의 내 매장 */
    List<Store> findByBrandIdOrderByCodeAsc(Long brandId);

    boolean existsByBrandIdAndCode(Long brandId, String code);
}
