package com.marinbom2.retailfnbplatform.order.api.dto;

import com.marinbom2.retailfnbplatform.order.domain.Order;
import com.marinbom2.retailfnbplatform.order.domain.OrderStatus;
import com.marinbom2.retailfnbplatform.order.domain.OrderType;
import com.marinbom2.retailfnbplatform.order.domain.PaymentMethod;
import java.time.Instant;
import java.util.List;

/** 주문 (md/api.md §6) */
public record OrderResponse(
        Long orderId,
        Long storeId,
        OrderType orderType,
        String tableLabel,
        OrderStatus status,
        List<Line> lines,
        long totalAmount,
        PaymentMethod paymentMethod,
        Instant paidAt,
        Instant createdAt
) {

    public record Line(Long skuId, String itemName, long unitPrice, long quantity, long lineAmount) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStoreId(), order.getOrderType(), order.getTableLabel(),
                order.getStatus(),
                order.getLines().stream()
                        .map(l -> new Line(l.getSkuId(), l.getItemName(), l.getUnitPrice().amount(), l.getQuantity(),
                                l.lineAmount().amount()))
                        .toList(),
                order.getTotalAmount().amount(), order.getPaymentMethod(), order.getPaidAt(), order.getCreatedAt());
    }
}
