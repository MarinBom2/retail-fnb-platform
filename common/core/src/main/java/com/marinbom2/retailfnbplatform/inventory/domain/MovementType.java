package com.marinbom2.retailfnbplatform.inventory.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 재고가 바뀐 이유. 수량을 바꾸는 길은 이 다섯 가지뿐이다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum MovementType implements DescriptionCode {

    RECEIVE("입고"),
    SALE("판매 차감"),
    SALE_CANCEL("판매 취소·환불 복원"),
    ADJUST("실사 조정"),
    TRANSFER_OUT("매장 간 이동 출고"),
    TRANSFER_IN("매장 간 이동 입고");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
