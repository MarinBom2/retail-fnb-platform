package com.marinbom2.retailfnbplatform.hq.api.dto;

import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreStatus;

/** 본사 매장 (md/api.md §8.2) */
public record HqStoreResponse(Long storeId, String code, String name, StoreStatus status, long staffCount) {

    public static HqStoreResponse of(Store store, long staffCount) {
        return new HqStoreResponse(store.getId(), store.getCode(), store.getName(), store.getStatus(), staffCount);
    }
}
