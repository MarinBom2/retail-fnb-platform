package com.marinbom2.retailfnbplatform.catalog.domain;

import com.marinbom2.retailfnbplatform.catalog.domain.exception.InvalidCatalogException;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매·재고 단위 (Stock Keeping Unit). 재고와 주문은 상품이 아니라 SKU 를 가리킨다.
 * Product 만 만든다 (Product#addSku).
 */
@Getter
@Entity
@Table(name = "sku", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sku_brand_code", columnNames = {"brand_id", "sku_code"}),
        @UniqueConstraint(name = "uk_sku_brand_barcode", columnNames = {"brand_id", "barcode"}),
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    /** 브랜드 안에서 SKU 코드·바코드가 겹치지 않게 하는 유니크 키용 복사본 */
    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Column(name = "sku_code", nullable = false, updatable = false, length = 50)
    private String skuCode;

    /** 바코드 (EAN-13 등). 음식점 메뉴처럼 없으면 null */
    @Column(length = 50)
    private String barcode;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false))
    private Money price;

    /** 의류만. 그 밖의 업종은 null */
    @Embedded
    private ApparelOption option;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockUnit unit;

    Sku(Product product, String skuCode, String barcode, Money price, ApparelOption option, StockUnit unit) {
        this.product = product;
        this.brandId = product.getBrandId();
        this.skuCode = Objects.requireNonNull(skuCode);
        this.barcode = barcode;
        this.price = price;
        this.option = option;
        this.unit = Objects.requireNonNull(unit);
    }

    /** 주문에 담을 수 있는지: 판매 중인 상품이고 식자재가 아님 */
    public boolean isSellable() {
        return product.isOnSale() && product.getKind().isSellable();
    }

    public ProductKind getKind() {
        return product.getKind();
    }

    /** 주문서·영수증에 남기는 이름. 의류는 옵션까지 붙인다 */
    public String displayName() {
        return option == null ? product.getName() : product.getName() + " (" + option.label() + ")";
    }

    /** 식자재는 팔지 않아 가격이 없다 */
    public void changePrice(Money price) {
        if (!product.getKind().isSellable()) {
            throw new InvalidCatalogException("식자재에는 판매 가격을 두지 않습니다: " + skuCode);
        }
        this.price = Objects.requireNonNull(price);
    }
}
