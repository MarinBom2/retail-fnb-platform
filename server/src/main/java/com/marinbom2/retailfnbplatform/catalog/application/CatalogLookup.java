package com.marinbom2.retailfnbplatform.catalog.application;

import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.SkuRepository;
import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 경로의 SKU 가 이 매장 브랜드의 것인지 확인하고 읽는다. 다른 브랜드 SKU 는 없는 것처럼 404 (존재를 알리지 않는다).
 */
@Component
@RequiredArgsConstructor
public class CatalogLookup {

    private final SkuRepository skuRepository;

    public Sku skuOf(Store store, Long skuId) {
        return skuRepository.findById(skuId)
                .filter(sku -> sku.getBrandId().equals(store.getBrandId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "상품을 찾을 수 없습니다."));
    }

    /** 재고를 세는 SKU 만 (메뉴는 재고가 없다) */
    public Sku stockTrackedSkuOf(Store store, Long skuId) {
        Sku sku = skuOf(store, skuId);
        if (!sku.tracksStock()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA, "재고를 세지 않는 상품입니다.");
        }
        return sku;
    }

    public List<Sku> skusOf(Store store, Collection<Long> skuIds) {
        List<Sku> skus = skuRepository.findAllById(skuIds).stream()
                .filter(sku -> sku.getBrandId().equals(store.getBrandId()))
                .toList();
        if (skus.size() != skuIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_DATA, "상품을 찾을 수 없습니다.");
        }
        return skus;
    }
}
