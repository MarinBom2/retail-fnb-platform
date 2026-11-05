package com.marinbom2.retailfnbplatform.store.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsByName(String name);

    /** 플랫폼 관리자의 브랜드 목록 */
    List<Brand> findAllByOrderByNameAsc();
}
