package com.marinbom2.retailfnbplatform.inventory.application;

import com.marinbom2.retailfnbplatform.inventory.api.dto.StoreAvailabilityResponse;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.store.domain.Store;
import com.marinbom2.retailfnbplatform.store.domain.StoreRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 같은 브랜드의 다른 영업 중 매장에서 이 SKU 재고를 찾는다 (FR-IN-006).
 * POS 품절, 재고 화면, 대시보드 안전재고 추천이 같은 규칙을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class StockAvailabilityFinder {

    private final StockRepository stockRepository;
    private final StoreRepository storeRepository;

    /** 재고 1 이상인 곳만, 많은 순 */
    public List<StoreAvailability> find(Store viewer, Long skuId) {
        List<Stock> stocks = stockRepository.findBySkuId(skuId).stream()
                .filter(s -> !s.getStoreId().equals(viewer.getId()) && s.getQuantity() > 0)
                .toList();
        Map<Long, Store> stores = storeRepository.findAllById(stocks.stream().map(Stock::getStoreId).toList()).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        return stocks.stream()
                .filter(s -> {
                    Store store = stores.get(s.getStoreId());
                    return store != null && store.getBrandId().equals(viewer.getBrandId()) && store.isOperating();
                })
                .map(s -> new StoreAvailability(stores.get(s.getStoreId()), s.getQuantity()))
                .sorted(Comparator.comparingLong(StoreAvailability::quantity).reversed())
                .toList();
    }

    public record StoreAvailability(Store store, long quantity) {

        public StoreAvailabilityResponse toResponse() {
            return new StoreAvailabilityResponse(store.getId(), store.getName(), quantity);
        }
    }
}
