package com.marinbom2.retailfnbplatform.store.application;

import com.marinbom2.retailfnbplatform.store.api.dto.MyStoreResponse;
import com.marinbom2.retailfnbplatform.store.domain.Brand;
import com.marinbom2.retailfnbplatform.store.domain.BrandRepository;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaff;
import com.marinbom2.retailfnbplatform.store.domain.StoreStaffRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-ST-003: 내가 일하는 매장 목록 */
@Service
@RequiredArgsConstructor
public class MyStoreService {

    private final StoreStaffRepository storeStaffRepository;
    private final StoreRepository storeRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<MyStoreResponse> myStores(Long memberId) {
        List<StoreStaff> staff = storeStaffRepository.findByMemberId(memberId);
        Map<Long, Store> stores = storeRepository.findAllById(staff.stream().map(StoreStaff::getStoreId).toList()).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        Map<Long, Brand> brands = brandRepository.findAllById(stores.values().stream().map(Store::getBrandId).distinct().toList())
                .stream().collect(Collectors.toMap(Brand::getId, Function.identity()));
        return staff.stream()
                .map(s -> {
                    Store store = stores.get(s.getStoreId());
                    return MyStoreResponse.of(store, brands.get(store.getBrandId()), s.getRole());
                })
                .sorted(Comparator.comparing(MyStoreResponse::brandName).thenComparing(MyStoreResponse::storeName))
                .toList();
    }
}
