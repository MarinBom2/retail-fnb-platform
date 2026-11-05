package com.marinbom2.retailfnbplatform.store.domain;

import com.marinbom2.retailfnbplatform.domain.base.BaseEntity;
import com.marinbom2.retailfnbplatform.store.domain.exception.IllegalStoreStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 매장. 브랜드에 속하고 업종을 브랜드에서 복사해 둔다 (주문·재고 규칙이 브랜드를 다시 읽지 않게).
 * 다른 애그리거트는 ID 로만 참조한다.
 */
@Getter
@Entity
@Table(name = "store", uniqueConstraints = @UniqueConstraint(name = "uk_store_brand_code", columnNames = {"brand_id", "code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false, updatable = false)
    private Long brandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, updatable = false, length = 30)
    private BusinessType businessType;

    /** 브랜드 안에서 매장을 구분하는 코드 (예: GN-0012) */
    @Column(nullable = false, updatable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status;

    /** @param brand 저장된 브랜드 (ID 가 있어야 한다) */
    public static Store create(Brand brand, String code, String name) {
        Store store = new Store();
        store.brandId = Objects.requireNonNull(brand.getId(), "저장된 브랜드여야 합니다");
        store.businessType = brand.getBusinessType();
        store.code = code;
        store.name = name;
        store.status = StoreStatus.PREPARING;
        return store;
    }

    /** 영업 시작. 준비 중이거나 영업 종료 상태에서만 */
    public void open() {
        if (status == StoreStatus.OPEN) {
            throw new IllegalStoreStateException(id, status, "open");
        }
        this.status = StoreStatus.OPEN;
    }

    /** 영업 종료. 영업 중일 때만 */
    public void close() {
        if (status != StoreStatus.OPEN) {
            throw new IllegalStoreStateException(id, status, "close");
        }
        this.status = StoreStatus.CLOSED;
    }

    public boolean isOperating() {
        return status == StoreStatus.OPEN;
    }

    public void rename(String name) {
        this.name = name;
    }
}
