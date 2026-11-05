package com.marinbom2.retailfnbplatform.catalog.application;

import com.marinbom2.retailfnbplatform.catalog.api.dto.CatalogProductResponse;
import com.marinbom2.retailfnbplatform.catalog.domain.ProductRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.store.application.StoreAccess;
import com.marinbom2.retailfnbplatform.store.application.StoreAccessGuard;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-CA-001: 브랜드 카탈로그 + 이 매장 재고. POS·상품 화면이 쓴다 */
@Service
@RequiredArgsConstructor
public class CatalogQueryService {

    private final StoreAccessGuard storeAccessGuard;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<CatalogProductResponse> catalog(Long storeId, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        Map<Long, Long> quantities = stockRepository.findByStoreId(storeId).stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getQuantity));
        return productRepository.findByBrandIdOrderByCategoryAscNameAsc(access.store().getBrandId()).stream()
                .map(product -> CatalogProductResponse.of(product, quantities))
                .toList();
    }
}
