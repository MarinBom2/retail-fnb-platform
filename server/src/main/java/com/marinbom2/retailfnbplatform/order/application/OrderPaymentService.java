package com.marinbom2.retailfnbplatform.order.application;

import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.inventory.domain.Stock;
import com.marinbom2.retailfnbplatform.inventory.domain.StockDeductionPolicy;
import com.marinbom2.retailfnbplatform.inventory.domain.StockMovementRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockReference;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRepository;
import com.marinbom2.retailfnbplatform.inventory.domain.StockRequirement;
import com.marinbom2.retailfnbplatform.inventory.domain.exception.InsufficientStockException;
import com.marinbom2.retailfnbplatform.inventory.policy.StockDeductionPolicies;
import com.marinbom2.retailfnbplatform.order.domain.Order;
import com.marinbom2.retailfnbplatform.order.domain.OrderRepository;
import com.marinbom2.retailfnbplatform.order.domain.PaymentMethod;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제·환불과 재고 변동을 한 트랜잭션으로 묶는다 (md/domain-design.md §6).
 * 재고가 모자라면 결제도 되지 않는다 (전부 롤백). 어떤 재고를 줄일지는 업종별 StockDeductionPolicy 가 정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockDeductionPolicies stockDeductionPolicies;
    private final Clock clock;

    /** @throws InsufficientStockException 한 SKU 라도 모자라면. 주문은 OPEN 으로 남는다 */
    @Transactional
    public Order pay(Long orderId, PaymentMethod method, Money paidAmount) {
        Order order = findOrder(orderId);
        order.pay(method, paidAmount, clock.instant());

        StockReference reference = StockReference.order(order.getId());
        requirements(order).forEach((skuId, quantity) -> {
            Stock stock = stockRepository.findByStoreIdAndSkuId(order.getStoreId(), skuId)
                    .orElseThrow(() -> new InsufficientStockException(order.getStoreId(), skuId, 0, quantity));
            stockMovementRepository.save(stock.deductForSale(quantity, reference));
        });
        log.info("[order] paid orderId={} storeId={}", order.getId(), order.getStoreId());
        return order;
    }

    /** 전액 환불. 결제 때 뺀 만큼 그대로 되돌린다 */
    @Transactional
    public Order refund(Long orderId) {
        Order order = findOrder(orderId);
        order.refund();

        StockReference reference = StockReference.order(order.getId());
        requirements(order).forEach((skuId, quantity) -> {
            Stock stock = stockRepository.findByStoreIdAndSkuId(order.getStoreId(), skuId)
                    .orElseGet(() -> stockRepository.save(Stock.empty(order.getStoreId(), skuId)));
            stockMovementRepository.save(stock.restoreForCancel(quantity, reference));
        });
        log.info("[order] refunded orderId={} storeId={}", order.getId(), order.getStoreId());
        return order;
    }

    /** 줄마다 정책을 적용하고 같은 SKU 를 합친다 (메뉴 두 개가 같은 식자재를 쓰는 경우) */
    private Map<Long, Long> requirements(Order order) {
        StockDeductionPolicy policy = stockDeductionPolicies.of(order.getBusinessType());
        Map<Long, Long> merged = new LinkedHashMap<>();
        order.getLines().forEach(line -> {
            List<StockRequirement> requirements = policy.requirementsFor(line.getSkuId(), line.getQuantity());
            requirements.forEach(r -> merged.merge(r.skuId(), r.quantity(), Math::addExact));
        });
        return merged;
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "주문을 찾을 수 없습니다."));
    }
}
