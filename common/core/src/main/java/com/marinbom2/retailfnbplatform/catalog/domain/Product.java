package com.marinbom2.retailfnbplatform.catalog.domain;

import com.marinbom2.retailfnbplatform.catalog.domain.exception.InvalidCatalogException;
import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 마스터 (애그리거트 루트). 브랜드 단위로 하나이고, 실제 판매·재고 단위는 그 아래 SKU 다.
 * SKU 옵션 규칙(의류는 색상×사이즈 필수, 같은 옵션 중복 금지)은 SKU 를 만드는 이 클래스 한 곳에서 지킨다.
 */
@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, updatable = false, length = 30)
    private BusinessType businessType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ProductKind kind;

    @Column(nullable = false, length = 200)
    private String name;

    /** 분류 (예: 주방용품, 티셔츠, 식사). 분류 체계는 정하지 않았다 (md/domain-design.md §14) */
    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sku> skus = new ArrayList<>();

    /** @param brand 저장된 브랜드. 업종이 이 종류를 허용해야 한다 */
    public static Product create(Brand brand, ProductKind kind, String name, String category) {
        BusinessType businessType = brand.getBusinessType();
        if (!kind.isAllowedFor(businessType)) {
            throw new InvalidCatalogException(businessType + " 브랜드는 " + kind + " 상품을 만들 수 없습니다");
        }
        Product product = new Product();
        product.brandId = Objects.requireNonNull(brand.getId(), "저장된 브랜드여야 합니다");
        product.businessType = businessType;
        product.kind = kind;
        product.name = name;
        product.category = category;
        product.status = ProductStatus.ON_SALE;
        return product;
    }

    /**
     * SKU 추가. 의류는 옵션이 반드시 있고 같은 색상·사이즈는 한 번만, 그 밖의 업종은 옵션이 없고 SKU 도 하나만이다.
     * 식자재는 팔지 않으므로 가격을 0 으로 둔다.
     */
    public Sku addSku(String skuCode, String barcode, Money price, ApparelOption option, StockUnit unit) {
        if (status == ProductStatus.DISCONTINUED) {
            throw new InvalidCatalogException("단종된 상품에는 SKU 를 더할 수 없습니다");
        }
        if (businessType.requiresApparelOption()) {
            if (option == null) {
                throw new InvalidCatalogException("의류 SKU 는 색상·사이즈가 필요합니다");
            }
            if (skus.stream().anyMatch(sku -> option.equals(sku.getOption()))) {
                throw new InvalidCatalogException("같은 색상·사이즈 SKU 가 이미 있습니다: " + option.label());
            }
        } else {
            if (option != null) {
                throw new InvalidCatalogException(businessType + " SKU 에는 색상·사이즈를 두지 않습니다");
            }
            if (!skus.isEmpty()) {
                throw new InvalidCatalogException(businessType + " 상품은 SKU 를 하나만 둡니다");
            }
        }
        if (skus.stream().anyMatch(sku -> sku.getSkuCode().equals(skuCode))) {
            throw new InvalidCatalogException("같은 SKU 코드가 이미 있습니다: " + skuCode);
        }
        Money effectivePrice = kind.canBeSold() ? Objects.requireNonNull(price, "가격이 필요합니다") : Money.ZERO;

        Sku sku = new Sku(this, skuCode, barcode, effectivePrice, option, unit);
        skus.add(sku);
        return sku;
    }

    public List<Sku> getSkus() {
        return Collections.unmodifiableList(skus);
    }

    public void suspend() {
        requireNotDiscontinued();
        this.status = ProductStatus.SUSPENDED;
    }

    public void resume() {
        requireNotDiscontinued();
        this.status = ProductStatus.ON_SALE;
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public boolean isOnSale() {
        return status == ProductStatus.ON_SALE;
    }

    private void requireNotDiscontinued() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new InvalidCatalogException("단종된 상품은 판매 상태를 바꿀 수 없습니다");
        }
    }
}
