package com.marinbom2.retailfnbplatform.inventory.api.dto;

/** 같은 브랜드 다른 매장의 재고 (md/api.md §4) */
public record StoreAvailabilityResponse(Long storeId, String storeName, long quantity) {
}
