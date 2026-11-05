package com.marinbom2.retailfnbplatform.store.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    /** 매장 단위 인가 (StoreAccessGuard), 중복 배정 확인 */
    Optional<StoreStaff> findByStoreIdAndMemberId(Long storeId, Long memberId);

    /** 내 매장 목록 */
    List<StoreStaff> findByMemberId(Long memberId);

    /** 본사: 매장 직원 목록 */
    List<StoreStaff> findByStoreIdOrderByIdAsc(Long storeId);

    /** 본사: 매장별 직원 수 */
    List<StoreStaff> findByStoreIdIn(Collection<Long> storeIds);
}
