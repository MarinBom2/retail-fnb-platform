package com.marinbom2.retailfnbplatform.catalog.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상품 종류. 리테일은 파는 물건이 곧 재고이고, 외식은 파는 것(메뉴)과 쌓아 두는 것(식자재)이 다르다.
 * 식자재도 SKU 로 두어 재고 모델을 하나로 쓴다 (md/domain-design.md §5.2).
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ProductKind implements DescriptionCode {

    MERCHANDISE("판매 상품"),
    MENU("메뉴"),
    INGREDIENT("식자재");

    private final String description;

    public boolean isAllowedFor(BusinessType businessType) {
        return switch (businessType) {
            case GENERAL_MERCHANDISE, APPAREL -> this == MERCHANDISE;
            case FOOD_SERVICE -> this == MENU || this == INGREDIENT;
        };
    }

    /** 주문에 담을 수 있는 종류인지. 식자재는 팔지 않는다 */
    public boolean isSellable() {
        return this != INGREDIENT;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
