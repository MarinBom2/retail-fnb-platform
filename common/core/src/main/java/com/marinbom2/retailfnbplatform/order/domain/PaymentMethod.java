package com.marinbom2.retailfnbplatform.order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 결제 수단. 실제 승인은 밖(PG·단말기)에서 하고 여기에는 결과만 남긴다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum PaymentMethod implements DescriptionCode {

    CARD("카드"),
    CASH("현금"),
    EASY_PAY("간편결제");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
