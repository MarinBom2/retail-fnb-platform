package com.marinbom2.retailfnbplatform.order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 주문 방식. 리테일은 계산대, 외식은 매장 식사(테이블)·포장 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum OrderType implements DescriptionCode {

    COUNTER("계산대"),
    DINE_IN("매장 식사"),
    TAKEOUT("포장");

    private final String description;

    public boolean isAllowedFor(BusinessType businessType) {
        return switch (businessType) {
            case GENERAL_MERCHANDISE, APPAREL -> this == COUNTER;
            case FOOD_SERVICE -> this == DINE_IN || this == TAKEOUT;
        };
    }

    /** 테이블 번호가 있어야 하는 주문인지 */
    public boolean requiresTable() {
        return this == DINE_IN;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
