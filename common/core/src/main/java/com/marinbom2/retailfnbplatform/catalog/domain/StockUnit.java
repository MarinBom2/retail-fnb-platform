package com.marinbom2.retailfnbplatform.catalog.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 재고 수량의 단위. 수량은 이 단위의 정수로만 센다 (소수 없음) */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum StockUnit implements DescriptionCode {

    EA("개"),
    GRAM("g"),
    MILLILITER("ml");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
