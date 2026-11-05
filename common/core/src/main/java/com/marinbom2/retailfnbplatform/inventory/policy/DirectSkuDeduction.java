package com.marinbom2.retailfnbplatform.inventory.policy;

import com.marinbom2.retailfnbplatform.inventory.domain.StockDeductionPolicy;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRequirement;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** 리테일: 판 SKU 를 판 수량만큼 그대로 뺀다. 생활용품·의류가 각각 빈 하나씩 쓴다 (InventoryPolicyConfig) */
@RequiredArgsConstructor
public class DirectSkuDeduction implements StockDeductionPolicy {

    private final BusinessType businessType;

    @Override
    public BusinessType businessType() {
        return businessType;
    }

    @Override
    public List<StockRequirement> requirementsFor(Long soldSkuId, long soldQuantity) {
        return List.of(new StockRequirement(soldSkuId, soldQuantity));
    }
}
