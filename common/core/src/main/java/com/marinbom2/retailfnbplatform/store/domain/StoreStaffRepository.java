package com.marinbom2.retailfnbplatform.store.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    /** 매장 단위 인가 (StoreAccessGuard) */
    Optional<StoreStaff> findByStoreIdAndMemberId(Long storeId, Long memberId);

    /** 내 매장 목록 */
    List<StoreStaff> findByMemberId(Long memberId);
}
