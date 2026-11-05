package com.marinbom2.retailfnbplatform.order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** OPEN → PAID → REFUNDED, OPEN → CANCELLED. 재고는 PAID 때 빠지고 REFUNDED 때 돌아온다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum OrderStatus implements DescriptionCode {

    OPEN("주문 중"),
    PAID("결제 완료"),
    CANCELLED("취소"),
    REFUNDED("환불");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
