package com.marinbom2.retailfnbplatform.store.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드 관리자 (본사). 브랜드의 매장·직원·상품을 등록하고, 그 브랜드의 모든 매장에 재고 권한까지 가진 채로 들어간다.
 * 브랜드를 만든 사람이 첫 관리자다 (md/api.md §8).
 */
@Getter
@Entity
@Table(name = "brand_admin",
        uniqueConstraints = @UniqueConstraint(name = "uk_brand_admin_member", columnNames = {"brand_id", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandAdmin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    public static BrandAdmin of(Brand brand, Long memberId) {
        BrandAdmin admin = new BrandAdmin();
        admin.brandId = Objects.requireNonNull(brand.getId(), "저장된 브랜드여야 합니다");
        admin.memberId = Objects.requireNonNull(memberId);
        return admin;
    }
}
