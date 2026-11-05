package com.marinbom2.retailfnbplatform.inventory.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 재고 변동의 출처 종류 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum StockReferenceType implements DescriptionCode {

    ORDER("주문"),
    TRANSFER("매장 간 이동"),
    MANUAL("수동 입력");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
