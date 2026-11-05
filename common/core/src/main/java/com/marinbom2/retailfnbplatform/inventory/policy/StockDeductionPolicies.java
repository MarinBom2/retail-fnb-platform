package com.marinbom2.retailfnbplatform.inventory.policy;

import com.marinbom2.retailfnbplatform.inventory.domain.StockDeductionPolicy;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 업종별 차감 정책을 고른다. 기동할 때 모든 업종에 정책이 정확히 하나씩 있는지 확인한다
 * (업종을 추가하고 구현체를 빠뜨리면 첫 판매가 아니라 기동에서 실패하게).
 */
public class StockDeductionPolicies {

    private final Map<BusinessType, StockDeductionPolicy> byType = new EnumMap<>(BusinessType.class);

    public StockDeductionPolicies(List<StockDeductionPolicy> policies) {
        for (StockDeductionPolicy policy : policies) {
            if (byType.put(policy.businessType(), policy) != null) {
                throw new IllegalStateException("업종 하나에 차감 정책이 둘 이상입니다: " + policy.businessType());
            }
        }
        for (BusinessType type : BusinessType.values()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException("차감 정책이 없는 업종입니다: " + type);
            }
        }
    }

    public StockDeductionPolicy of(BusinessType businessType) {
        return byType.get(businessType);
    }
}
