package com.marinbom2.retailfnbplatform.store.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;
import com.marinbom2.retailfnbplatform.store.domain.StoreStatus;
import lombok.Getter;

/** 매장 상태에서 할 수 없는 일 (영업 종료 매장에서 주문, 이미 영업 중인데 다시 open 등) */
@Getter
public class IllegalStoreStateException extends DomainException {

    private final StoreStatus status;

    public IllegalStoreStateException(Long storeId, StoreStatus status, String action) {
        super("store " + storeId + " is " + status + ", cannot " + action);
        this.status = status;
    }
}
