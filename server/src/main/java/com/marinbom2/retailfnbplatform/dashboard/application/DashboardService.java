package com.marinbom2.retailfnbplatform.dashboard.application;

import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.catalog.domain.SkuRepository;
import com.marinbom2.retailfnbplatform.config.PlatformProperties;
import com.marinbom2.retailfnbplatform.dashboard.api.dto.DashboardResponse;
import com.marinbom2.retailfnbplatform.dashboard.api.dto.DashboardResponse.HourlySales;
import com.marinbom2.retailfnbplatform.dashboard.api.dto.DashboardResponse.LowStockItem;
import com.marinbom2.retailfnbplatform.dashboard.api.dto.DashboardResponse.TopItem;
import com.marinbom2.retailfnbplatform.inventory.application.StockAvailabilityFinder;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockTransferRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.TransferStatus;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.order.domain.Order;
import com.marinbom2.retailfnbplatform.order.domain.OrderLine;
import com.marinbom2.retailfnbplatform.order.domain.OrderRepository;
import com.marinbom2.retailfnbplatform.order.domain.OrderStatus;
import com.marinbom2.retailfnbplatform.store.application.StoreAccess;
import com.marinbom2.retailfnbplatform.store.application.StoreAccessGuard;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-DA-001: 오늘(영업일) 매장 현황. 매출은 결제 시각 기준이고 환불된 주문은 매출에서 빼 따로 보여 준다.
 * 지금은 주문을 읽어 메모리에서 집계한다. 매장당 하루 주문이 많아지면 집계 쿼리·일 마감 배치로 옮긴다 (md/domain-design.md §14).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TOP_ITEMS = 5;
    private static final int LOW_STOCK_ITEMS = 10;

    private final StoreAccessGuard storeAccessGuard;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final SkuRepository skuRepository;
    private final StockTransferRepository transferRepository;
    private final StockAvailabilityFinder stockAvailabilityFinder;
    private final PlatformProperties platformProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse today(Long storeId, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        ZoneId zone = platformProperties.zoneId();
        LocalDate today = LocalDate.now(clock.withZone(zone));

        List<Order> paidToday = orderRepository.findByStoreIdAndPaidAtGreaterThanEqualAndPaidAtLessThan(storeId,
                today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
        List<Order> sales = paidToday.stream().filter(o -> o.getStatus() == OrderStatus.PAID).toList();
        long salesAmount = sales.stream().mapToLong(o -> o.getTotalAmount().amount()).sum();
        long refunded = paidToday.stream().filter(o -> o.getStatus() == OrderStatus.REFUNDED)
                .mapToLong(o -> o.getTotalAmount().amount()).sum();

        long[] byHour = new long[24];
        sales.forEach(o -> byHour[o.getPaidAt().atZone(zone).getHour()] += o.getTotalAmount().amount());

        return new DashboardResponse(storeId, today, salesAmount, sales.size(),
                sales.isEmpty() ? 0 : salesAmount / sales.size(), refunded,
                IntStream.range(0, 24).mapToObj(h -> new HourlySales(h, byHour[h])).toList(),
                topItems(sales),
                lowStock(access),
                orderRepository.countByStoreIdAndStatus(storeId, OrderStatus.OPEN),
                transferRepository.countByFromStoreIdAndStatus(storeId, TransferStatus.REQUESTED),
                transferRepository.countByFromStoreIdAndStatus(storeId, TransferStatus.APPROVED),
                transferRepository.countByToStoreIdAndStatus(storeId, TransferStatus.SHIPPED));
    }

    private static List<TopItem> topItems(List<Order> sales) {
        Map<Long, TopItem> bySku = new LinkedHashMap<>();
        sales.stream().flatMap(o -> o.getLines().stream()).forEach((OrderLine line) ->
                bySku.merge(line.getSkuId(),
                        new TopItem(line.getSkuId(), line.getItemName(), line.getQuantity(), line.lineAmount().amount()),
                        (a, b) -> new TopItem(a.skuId(), a.itemName(), a.quantity() + b.quantity(), a.amount() + b.amount())));
        return bySku.values().stream()
                .sorted(Comparator.comparingLong(TopItem::quantity).reversed().thenComparing(TopItem::amount, Comparator.reverseOrder()))
                .limit(TOP_ITEMS)
                .toList();
    }

    /** 안전재고 이하 품목과, 같은 브랜드에서 재고가 가장 많은 매장 (이동 요청 추천). 안전재고 대비 많이 모자란 순 */
    private List<LowStockItem> lowStock(StoreAccess access) {
        List<Stock> low = stockRepository.findByStoreId(access.storeId()).stream()
                .filter(Stock::needsReorder)
                .sorted(Comparator.comparingDouble(s -> (double) s.getQuantity() / s.getReorderPoint()))
                .limit(LOW_STOCK_ITEMS)
                .toList();
        Map<Long, Sku> skus = skuRepository.findAllById(low.stream().map(Stock::getSkuId).toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        return low.stream().map(stock -> {
            Sku sku = skus.get(stock.getSkuId());
            var best = stockAvailabilityFinder.find(access.store(), stock.getSkuId()).stream().findFirst();
            return new LowStockItem(stock.getSkuId(), sku.displayName(), stock.getQuantity(), stock.getReorderPoint(), sku.getUnit(),
                    best.map(b -> b.store().getId()).orElse(null),
                    best.map(b -> b.store().getName()).orElse(null),
                    best.map(StockAvailabilityFinder.StoreAvailability::quantity).orElse(null));
        }).toList();
    }
}
