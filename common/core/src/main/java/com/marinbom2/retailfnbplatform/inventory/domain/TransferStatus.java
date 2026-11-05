package com.marinbom2.retailfnbplatform.inventory.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 매장 간 이동 상태. REQUESTED → APPROVED → SHIPPED → RECEIVED, 중간에 REJECTED·CANCELLED 로 끝날 수 있다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum TransferStatus implements DescriptionCode {

    REQUESTED("승인 대기"),
    APPROVED("출고 대기"),
    REJECTED("거절"),
    SHIPPED("입고 대기"),
    RECEIVED("입고 완료"),
    CANCELLED("취소");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
