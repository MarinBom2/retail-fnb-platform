package com.marinbom2.retailfnbplatform.inventory.domain;

import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.List;

/**
 * 판매한 SKU 가 어떤 재고를 줄이는지 정하는 확장점 (md/solid.md O).
 * 리테일은 판 SKU 그대로, 외식은 레시피의 식자재다. 업종 하나에 구현체(빈) 하나.
 *
 * 규약 (구현체를 바꿔도 같다)
 *   - 돌려준 목록에 같은 SKU 가 두 번 나오지 않는다
 *   - 줄일 재고가 없으면 예외가 아니라 빈 목록이다 (예: 레시피를 아직 등록하지 않은 메뉴)
 */
public interface StockDeductionPolicy {

    BusinessType businessType();

    /** @param soldQuantity 판매 수량 (1 이상) */
    List<StockRequirement> requirementsFor(Long soldSkuId, long soldQuantity);
}
