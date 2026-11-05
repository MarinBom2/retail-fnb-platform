package com.marinbom2.retailfnbplatform.store.application;

import com.marinbom2.retailfnbplatform.store.domain.StaffRole;
import com.marinbom2.retailfnbplatform.store.domain.Store;

/**
 * 인가를 통과한 매장과 그 매장에서의 내 역할.
 *
 * @param role 플랫폼 관리자면 null (매장 직원이 아니어도 들어온다)
 */
public record StoreAccess(Store store, StaffRole role, boolean canManageInventory) {

    public Long storeId() {
        return store.getId();
    }
}
