package com.marinbom2.retailfnbplatform.store.application;

import com.marinbom2.retailfnbplatform.store.api.dto.MyStoreResponse;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdmin;
import com.marinbom2.retailfnbplatform.store.domain.BrandAdminRepository;
import com.marinbom2.retailfnbplatform.store.domain.BrandRepository;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaff;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaffRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-ST-003: 내가 일하는 매장 목록. 직원으로 배정된 매장 + 내가 관리하는 브랜드의 모든 매장.
 * 같은 매장이 둘 다에 걸리면 본사 관리자가 우선한다 (권한이 더 넓다).
 */
@Service
@RequiredArgsConstructor
public class MyStoreService {

    private final StoreStaffRepository storeStaffRepository;
    private final BrandAdminRepository brandAdminRepository;
    private final StoreRepository storeRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<MyStoreResponse> myStores(Long memberId) {
        Set<Long> adminBrandIds = brandAdminRepository.findByMemberId(memberId).stream()
                .map(BrandAdmin::getBrandId).collect(Collectors.toSet());
        List<StoreStaff> staff = storeStaffRepository.findByMemberId(memberId);

        Map<Long, Store> stores = new LinkedHashMap<>();
        adminBrandIds.forEach(brandId -> storeRepository.findByBrandIdOrderByCodeAsc(brandId)
                .forEach(store -> stores.put(store.getId(), store)));
        storeRepository.findAllById(staff.stream().map(StoreStaff::getStoreId).toList())
                .forEach(store -> stores.putIfAbsent(store.getId(), store));
        Map<Long, Brand> brands = brandRepository.findAllById(stores.values().stream().map(Store::getBrandId).distinct().toList())
                .stream().collect(Collectors.toMap(Brand::getId, Function.identity()));
        Map<Long, StoreStaff> staffByStore = staff.stream().collect(Collectors.toMap(StoreStaff::getStoreId, Function.identity()));

        return stores.values().stream()
                .map(store -> {
                    Brand brand = brands.get(store.getBrandId());
                    return adminBrandIds.contains(store.getBrandId())
                            ? MyStoreResponse.asBrandAdmin(store, brand)
                            : MyStoreResponse.asStaff(store, brand, staffByStore.get(store.getId()).getRole());
                })
                .sorted(Comparator.comparing(MyStoreResponse::brandName).thenComparing(MyStoreResponse::storeName))
                .toList();
    }
}
