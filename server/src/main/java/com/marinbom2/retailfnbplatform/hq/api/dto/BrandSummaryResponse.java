package com.marinbom2.retailfnbplatform.hq.api.dto;

import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BusinessType;

/** 본사 브랜드 요약 (md/api.md §8.1) */
public record BrandSummaryResponse(
        Long brandId,
        String name,
        BusinessType businessType,
        long storeCount,
        long openStoreCount,
        long productCount
) {

    public static BrandSummaryResponse of(Brand brand, long storeCount, long openStoreCount, long productCount) {
        return new BrandSummaryResponse(brand.getId(), brand.getName(), brand.getBusinessType(), storeCount, openStoreCount, productCount);
    }
}
