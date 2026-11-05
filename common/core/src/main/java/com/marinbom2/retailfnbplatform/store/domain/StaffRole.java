package com.marinbom2.retailfnbplatform.store.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 매장 안에서의 역할. 플랫폼 권한(RoleType)과 별개다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum StaffRole implements DescriptionCode {

    OWNER("점주"),
    MANAGER("매니저"),
    STAFF("직원");

    private final String description;

    /** 입고·실사·매장 간 이동처럼 재고 수량을 직접 바꾸는 일 */
    public boolean canManageInventory() {
        return this == OWNER || this == MANAGER;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
