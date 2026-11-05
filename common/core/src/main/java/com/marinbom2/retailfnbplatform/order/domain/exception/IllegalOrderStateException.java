package com.marinbom2.retailfnbplatform.order.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;
import com.marinbom2.retailfnbplatform.order.domain.OrderStatus;
import lombok.Getter;

/** 지금 주문 상태에서 할 수 없는 일 (결제 끝난 주문에 담기, 두 번 결제 등) */
@Getter
public class IllegalOrderStateException extends DomainException {

    private final OrderStatus status;

    public IllegalOrderStateException(Long orderId, OrderStatus status, String action) {
        super("order " + orderId + " is " + status + ", cannot " + action);
        this.status = status;
    }
}
