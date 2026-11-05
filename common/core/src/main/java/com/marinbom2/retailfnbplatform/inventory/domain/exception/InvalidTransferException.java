package com.marinbom2.retailfnbplatform.inventory.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;

/** 이동 요청 내용 규칙 위반 (다른 브랜드, 같은 매장, 빈 줄, 재고를 세지 않는 SKU 등) */
public class InvalidTransferException extends DomainException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
