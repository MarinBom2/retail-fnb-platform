package com.marinbom2.retailfnbplatform.store.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 브랜드 (다이소, 지오다노, OO식당). 상품 마스터와 매장을 묶는 단위이고 업종은 바뀌지 않는다 */
@Getter
@Entity
@Table(name = "brand", uniqueConstraints = @UniqueConstraint(name = "uk_brand_name", columnNames = "name"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, updatable = false, length = 30)
    private BusinessType businessType;

    public static Brand register(String name, BusinessType businessType) {
        Brand brand = new Brand();
        brand.name = requireText(name);
        brand.businessType = businessType;
        return brand;
    }

    public void rename(String name) {
        this.name = requireText(name);
    }

    private static String requireText(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("브랜드 이름이 필요합니다");
        }
        return name.trim();
    }
}
