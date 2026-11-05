package com.marinbom2.retailfnbplatform.hq.api.dto;

import com.marinbom2.retailfnbplatform.store.domain.StaffRole;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaff;

/** 매장 직원 (md/api.md §8.2) */
public record StoreStaffResponse(Long staffId, Long memberId, String email, StaffRole role) {

    public static StoreStaffResponse of(StoreStaff staff, String email) {
        return new StoreStaffResponse(staff.getId(), staff.getMemberId(), email, staff.getRole());
    }
}
