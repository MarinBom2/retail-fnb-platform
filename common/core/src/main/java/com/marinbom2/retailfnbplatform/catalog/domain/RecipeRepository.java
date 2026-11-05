package com.marinbom2.retailfnbplatform.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** 메뉴가 팔릴 때 차감할 식자재 (RecipeStockDeduction), 본사 레시피 편집 */
    Optional<Recipe> findByMenuSkuId(Long menuSkuId);

    /** 본사 상품 목록의 레시피 유무 */
    List<Recipe> findByMenuSkuIdIn(Collection<Long> menuSkuIds);
}
