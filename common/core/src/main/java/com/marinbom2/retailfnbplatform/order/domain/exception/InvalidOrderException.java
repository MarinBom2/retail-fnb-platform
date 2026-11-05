package com.marinbom2.retailfnbplatform.order.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;

/** 주문 내용 규칙 위반 (업종에 없는 주문 방식, 팔 수 없는 상품, 결제 금액 불일치 등) */
public class InvalidOrderException extends DomainException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
