package com.marinbom2.retailfnbplatform.inventory.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.retailfnbplatform.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 매장 간 이동에서 매장이 할 수 있는 동작. 누가 언제 할 수 있는지는 StockTransfer#availableActions 한 곳에서 정한다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum TransferAction implements DescriptionCode {

    APPROVE("승인"),
    REJECT("거절"),
    SHIP("출고"),
    RECEIVE("입고"),
    CANCEL("취소");

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }
}
