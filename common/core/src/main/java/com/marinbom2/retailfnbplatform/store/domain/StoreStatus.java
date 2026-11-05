package com.marinbom2.retailfnbplatform.store.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 매장 상태. PREPARING → OPEN ⇄ CLOSED. 판매는 OPEN 에서만 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum StoreStatus implements DescriptionCode {

    PREPARING("오픈 준비"),
    OPEN("영업 중"),
    CLOSED("영업 종료");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
