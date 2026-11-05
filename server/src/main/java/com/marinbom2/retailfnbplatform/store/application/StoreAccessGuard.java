package com.marinbom2.retailfnbplatform.store.application;

import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdminRepository;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaffRepository;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 매장 단위 인가 (md/api.md §0). 매장 API 는 서비스 첫 줄에서 이것을 부른다.
 * 플랫폼 관리자(ADMIN, SYSTEM)와 그 브랜드의 관리자(본사)는 모든 매장에 들어오고 재고 권한도 있다.
 */
@Component
@RequiredArgsConstructor
public class StoreAccessGuard {

    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final BrandAdminRepository brandAdminRepository;

    /** @throws BusinessException NOT_FOUND_DATA(없는 매장), FORBIDDEN(이 매장 직원이 아님) */
    public StoreAccess require(Long storeId, Member member) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "매장을 찾을 수 없습니다."));
        if (member.isAdmin() || brandAdminRepository.existsByBrandIdAndMemberId(store.getBrandId(), member.getId())) {
            return new StoreAccess(store, null, true);
        }
        return storeStaffRepository.findByStoreIdAndMemberId(storeId, member.getId())
                .map(staff -> new StoreAccess(store, staff.getRole(), staff.getRole().canManageInventory()))
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "이 매장에 접근할 권한이 없습니다."));
    }

    /** 입고·실사·안전재고·매장 간 이동처럼 재고를 바꾸는 일. 점주·매니저만 */
    public StoreAccess requireInventoryManager(Long storeId, Member member) {
        StoreAccess access = require(storeId, member);
        if (!access.canManageInventory()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "재고를 바꿀 권한이 없습니다. 점주나 매니저에게 요청해 주세요.");
        }
        return access;
    }
}
