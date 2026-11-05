package com.marinbom2.retailfnbplatform.store.api.dto;

import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import com.marinbom2.retailfnbplatform.store.domain.StaffRole;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 내가 일하는 매장 (md/api.md §1). 역할은 매장 역할이거나 본사 관리자다 */
@Schema(description = "내가 일하는 매장")
public record MyStoreResponse(
        Long storeId,
        String storeCode,
        String storeName,
        StoreStatus status,
        Long brandId,
        String brandName,
        BusinessType businessType,
        RoleLabel role,
        boolean canManageInventory
) {

    /** enum 과 같은 {code, description} 모양. 본사 관리자는 매장 역할 enum 에 없어서 따로 만든다 */
    public record RoleLabel(String code, String description) {

        public static final RoleLabel HQ_ADMIN = new RoleLabel("HQ_ADMIN", "본사 관리자");

        public static RoleLabel of(StaffRole role) {
            return new RoleLabel(role.name(), role.getDescription());
        }
    }

    public static MyStoreResponse asStaff(Store store, Brand brand, StaffRole role) {
        return new MyStoreResponse(store.getId(), store.getCode(), store.getName(), store.getStatus(),
                brand.getId(), brand.getName(), store.getBusinessType(), RoleLabel.of(role), role.canManageInventory());
    }

    public static MyStoreResponse asBrandAdmin(Store store, Brand brand) {
        return new MyStoreResponse(store.getId(), store.getCode(), store.getName(), store.getStatus(),
                brand.getId(), brand.getName(), store.getBusinessType(), RoleLabel.HQ_ADMIN, true);
    }
}
