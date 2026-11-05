package com.marinbom2.retailfnbplatform.inventory.policy;

import com.marinbom2.retailfnbplatform.catalog.domain.RecipeRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockDeductionPolicy;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 업종별 재고 차감 정책 빈. 업종을 더하면 여기에 빈 하나를 더한다 */
@Configuration
public class InventoryPolicyConfig {

    @Bean
    public StockDeductionPolicy generalMerchandiseDeduction() {
        return new DirectSkuDeduction(BusinessType.GENERAL_MERCHANDISE);
    }

    @Bean
    public StockDeductionPolicy apparelDeduction() {
        return new DirectSkuDeduction(BusinessType.APPAREL);
    }

    @Bean
    public StockDeductionPolicy foodServiceDeduction(RecipeRepository recipeRepository) {
        return new RecipeStockDeduction(recipeRepository);
    }

    @Bean
    public StockDeductionPolicies stockDeductionPolicies(List<StockDeductionPolicy> policies) {
        return new StockDeductionPolicies(policies);
    }
}
