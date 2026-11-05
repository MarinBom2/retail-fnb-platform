package com.marinbom2.retailfnbplatform.inventory.application;

import com.marinbom2.retailfnbplatform.catalog.domain.Product;
import com.marinbom2.retailfnbplatform.catalog.application.CatalogLookup;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductRepository;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.inventory.api.dto.StockItemResponse;
import com.marinbom2.retailfnbplatform.inventory.api.dto.StockMovementResponse;
import com.marinbom2.retailfnbplatform.inventory.api.dto.StoreAvailabilityResponse;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovementRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockReference;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.application.StoreAccess;
import com.marinbom2.retailfnbplatform.store.application.StoreAccessGuard;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 매장 재고 조회·입고·실사·안전재고 (FR-IN-001, 002, 004, 005, 006) */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StoreAccessGuard storeAccessGuard;
    private final CatalogLookup catalogLookup;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockAvailabilityFinder stockAvailabilityFinder;

    /** 재고를 세는 SKU 전부. 입고 전 SKU 도 0 으로 보여 준다 */
    @Transactional(readOnly = true)
    public List<StockItemResponse> list(Long storeId, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        Map<Long, Stock> stocks = stockRepository.findByStoreId(storeId).stream()
                .collect(Collectors.toMap(Stock::getSkuId, Function.identity()));
        return productRepository.findByBrandIdOrderByCategoryAscNameAsc(access.store().getBrandId()).stream()
                .filter(p -> p.getKind().tracksStock())
                .map(Product::getSkus)
                .flatMap(List::stream)
                .map(sku -> StockItemResponse.of(sku, stocks.get(sku.getId())))
                .toList();
    }

    @Transactional
    public StockItemResponse receive(Long storeId, Long skuId, long quantity, Member member) {
        StoreAccess access = storeAccessGuard.requireInventoryManager(storeId, member);
        Sku sku = catalogLookup.stockTrackedSkuOf(access.store(), skuId);
        Stock stock = stockOf(storeId, skuId);
        stockMovementRepository.save(stock.receive(quantity, StockReference.manual()));
        log.info("[stock] receive storeId={} skuId={} qty={}", storeId, skuId, quantity);
        return StockItemResponse.of(sku, stock);
    }

    @Transactional
    public StockItemResponse adjust(Long storeId, Long skuId, long countedQuantity, Member member) {
        StoreAccess access = storeAccessGuard.requireInventoryManager(storeId, member);
        Sku sku = catalogLookup.stockTrackedSkuOf(access.store(), skuId);
        Stock stock = stockOf(storeId, skuId);
        stockMovementRepository.save(stock.adjustTo(countedQuantity, StockReference.manual()));
        log.info("[stock] adjust storeId={} skuId={} counted={}", storeId, skuId, countedQuantity);
        return StockItemResponse.of(sku, stock);
    }

    @Transactional
    public StockItemResponse changeReorderPoint(Long storeId, Long skuId, long reorderPoint, Member member) {
        StoreAccess access = storeAccessGuard.requireInventoryManager(storeId, member);
        Sku sku = catalogLookup.stockTrackedSkuOf(access.store(), skuId);
        Stock stock = stockOf(storeId, skuId);
        stock.changeReorderPoint(reorderPoint);
        return StockItemResponse.of(sku, stock);
    }

    @Transactional(readOnly = true)
    public List<StoreAvailabilityResponse> availability(Long storeId, Long skuId, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        catalogLookup.stockTrackedSkuOf(access.store(), skuId);
        return stockAvailabilityFinder.find(access.store(), skuId).stream()
                .map(StockAvailabilityFinder.StoreAvailability::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> movements(Long storeId, Long skuId, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        catalogLookup.stockTrackedSkuOf(access.store(), skuId);
        return stockRepository.findByStoreIdAndSkuId(storeId, skuId)
                .map(stock -> stockMovementRepository.findTop50ByStockIdOrderByIdDesc(stock.getId()).stream()
                        .map(StockMovementResponse::from).toList())
                .orElse(List.of());
    }

    /** 처음 입고 전이면 빈 재고를 만들어 저장한다 (원장이 재고 ID 를 가리켜야 해서 먼저 저장) */
    private Stock stockOf(Long storeId, Long skuId) {
        return stockRepository.findByStoreIdAndSkuId(storeId, skuId)
                .orElseGet(() -> stockRepository.save(Stock.empty(storeId, skuId)));
    }
}
