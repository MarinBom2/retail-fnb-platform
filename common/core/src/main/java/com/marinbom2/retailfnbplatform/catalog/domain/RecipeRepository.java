package com.marinbom2.retailfnbplatform.catalog.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** 메뉴가 팔릴 때 차감할 식자재 (RecipeStockDeduction) */
    Optional<Recipe> findByMenuSkuId(Long menuSkuId);
}
