package com.marinbom2.retailfnbplatform.store.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 업종. 브랜드가 정하고 바뀌지 않는다. 업종마다 달라지는 규칙은 여기 플래그와 확장점(StockDeductionPolicy)으로만 나눈다.
 * 새 업종은 값 하나 + 필요한 확장점 구현체를 더한다 (md/domain-design.md §3).
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum BusinessType implements DescriptionCode {

    /** 예: 다이소. 단일 SKU, 바코드 판매 */
    GENERAL_MERCHANDISE("생활용품 리테일"),
    /** 예: 지오다노. 상품(스타일) × 색상 × 사이즈 SKU */
    APPAREL("의류 리테일"),
    /** 예: 음식점. 메뉴 판매, 식자재 재고, 테이블 */
    FOOD_SERVICE("외식");

    private final String description;

    /** SKU 에 색상·사이즈 옵션이 반드시 있어야 하는지 */
    public boolean requiresApparelOption() {
        return this == APPAREL;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
