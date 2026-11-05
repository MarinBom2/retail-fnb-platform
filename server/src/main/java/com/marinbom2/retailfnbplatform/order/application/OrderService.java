package com.marinbom2.retailfnbplatform.order.application;

import com.marinbom2.retailfnbplatform.catalog.application.CatalogLookup;
import com.marinbom2.retailfnbplatform.catalog.domain.Sku;
import com.marinbom2.retailfnbplatform.domain.base.Money;
import com.marinbom2.retailfnbplatform.exception.BusinessException;
import com.marinbom2.retailfnbplatform.member.domain.Member;
import com.marinbom2.retailfnbplatform.order.api.dto.OpenOrderRequest;
import com.marinbom2.retailfnbplatform.order.api.dto.OrderItemRequest;
import com.marinbom2.retailfnbplatform.order.api.dto.OrderResponse;
import com.marinbom2.retailfnbplatform.order.domain.Order;
import com.marinbom2.retailfnbplatform.order.domain.OrderRepository;
import com.marinbom2.retailfnbplatform.order.domain.OrderStatus;
import com.marinbom2.retailfnbplatform.order.domain.OrderType;
import com.marinbom2.retailfnbplatform.order.domain.PaymentMethod;
import com.marinbom2.retailfnbplatform.store.application.StoreAccess;
import com.marinbom2.retailfnbplatform.store.application.StoreAccessGuard;
import com.marinbom2.retailfnbplatform.type.common.EnumCodes;
import com.marinbom2.retailfnbplatform.type.common.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** POS 주문 (FR-OR-001~005). 결제·환불의 재고 변동은 OrderPaymentService 가 같은 트랜잭션에서 한다 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final StoreAccessGuard storeAccessGuard;
    private final CatalogLookup catalogLookup;
    private final OrderRepository orderRepository;
    private final OrderPaymentService orderPaymentService;

    @Transactional
    public OrderResponse open(Long storeId, OpenOrderRequest request, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        Order order = Order.open(access.store(), EnumCodes.parse(OrderType.class, request.orderType()), request.tableLabel());
        Map<Long, Sku> skus = catalogLookup.skusOf(access.store(), request.items().stream().map(OrderItemRequest::skuId).toList())
                .stream().collect(Collectors.toMap(Sku::getId, Function.identity()));
        request.items().forEach(item -> order.addItem(skus.get(item.skuId()), item.quantity()));
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(Long storeId, String status, Member member) {
        storeAccessGuard.require(storeId, member);
        List<Order> orders = status == null || status.isBlank()
                ? orderRepository.findTop50ByStoreIdOrderByIdDesc(storeId)
                : orderRepository.findTop50ByStoreIdAndStatusOrderByIdDesc(storeId, EnumCodes.parse(OrderStatus.class, status));
        return orders.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long storeId, Long orderId, Member member) {
        storeAccessGuard.require(storeId, member);
        return OrderResponse.from(orderOf(storeId, orderId));
    }

    @Transactional
    public OrderResponse addItem(Long storeId, Long orderId, OrderItemRequest request, Member member) {
        StoreAccess access = storeAccessGuard.require(storeId, member);
        Order order = orderOf(storeId, orderId);
        order.addItem(catalogLookup.skuOf(access.store(), request.skuId()), request.quantity());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse changeQuantity(Long storeId, Long orderId, Long skuId, long quantity, Member member) {
        storeAccessGuard.require(storeId, member);
        Order order = orderOf(storeId, orderId);
        order.changeQuantity(skuId, quantity);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse removeItem(Long storeId, Long orderId, Long skuId, Member member) {
        storeAccessGuard.require(storeId, member);
        Order order = orderOf(storeId, orderId);
        order.removeItem(skuId);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse pay(Long storeId, Long orderId, String method, long amount, Member member) {
        storeAccessGuard.require(storeId, member);
        orderOf(storeId, orderId);
        return OrderResponse.from(orderPaymentService.pay(orderId, EnumCodes.parse(PaymentMethod.class, method), Money.won(amount)));
    }

    @Transactional
    public OrderResponse cancel(Long storeId, Long orderId, Member member) {
        storeAccessGuard.require(storeId, member);
        Order order = orderOf(storeId, orderId);
        order.cancel();
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse refund(Long storeId, Long orderId, Member member) {
        storeAccessGuard.require(storeId, member);
        orderOf(storeId, orderId);
        return OrderResponse.from(orderPaymentService.refund(orderId));
    }

    /** 다른 매장 주문은 없는 것처럼 404 */
    private Order orderOf(Long storeId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getStoreId().equals(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA, "주문을 찾을 수 없습니다."));
    }
}
