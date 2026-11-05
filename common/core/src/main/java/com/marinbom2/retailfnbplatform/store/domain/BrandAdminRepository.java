package com.marinbom2.retailfnbplatform.store.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandAdminRepository extends JpaRepository<BrandAdmin, Long> {

    /** 본사 인가 (BrandAccessGuard), 매장 인가 (StoreAccessGuard) */
    boolean existsByBrandIdAndMemberId(Long brandId, Long memberId);

    /** 내 브랜드, 내 매장 목록 */
    List<BrandAdmin> findByMemberId(Long memberId);
}
