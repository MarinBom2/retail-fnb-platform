package com.marinbom2.retailfnbplatform.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** 브랜드 카탈로그. SKU 를 함께 읽는다 (화면마다 상품 × SKU 를 다 그린다) */
    @EntityGraph(attributePaths = "skus")
    List<Product> findByBrandIdOrderByCategoryAscNameAsc(Long brandId);
}
