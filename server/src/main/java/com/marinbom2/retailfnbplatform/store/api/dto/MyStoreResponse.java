package com.marinbom2.retailfnbplatform.store.api.dto;

import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.StaffRole;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 일하는 매장")
public record MyStoreResponse(
        Long storeId,
        String storeCode,
        String storeName,
        StoreStatus status,
        Long brandId,
        String brandName,
        BusinessType businessType,
        StaffRole role,
        boolean canManageInventory
) {

    public static MyStoreResponse of(Store store, Brand brand, StaffRole role) {
        return new MyStoreResponse(store.getId(), store.getCode(), store.getName(), store.getStatus(),
                brand.getId(), brand.getName(), store.getBusinessType(), role, role.canManageInventory());
    }
}
