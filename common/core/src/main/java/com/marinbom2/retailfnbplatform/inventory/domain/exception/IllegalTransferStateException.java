package com.marinbom2.retailfnbplatform.inventory.domain.exception;

import com.marinbom2.retailfnbplatform.domain.base.DomainException;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferAction;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferStatus;

/** 지금 이동 상태이거나 이 매장 쪽에서는 할 수 없는 동작 (출고 전 입고, 보낸 매장이 입고 등) */
public class IllegalTransferStateException extends DomainException {

    public IllegalTransferStateException(Long transferId, TransferStatus status, TransferAction action) {
        super("transfer " + transferId + " is " + status + ", cannot " + action);
    }
}
