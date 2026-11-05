package com.marinbom2.retailfnbplatform.order.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findTop50ByStoreIdOrderByIdDesc(Long storeId);

    List<Order> findTop50ByStoreIdAndStatusOrderByIdDesc(Long storeId, OrderStatus status);

    /** 대시보드: 영업일 안에 결제된 주문 (이후 환불된 것 포함) */
    List<Order> findByStoreIdAndPaidAtGreaterThanEqualAndPaidAtLessThan(Long storeId, Instant from, Instant to);

    long countByStoreIdAndStatus(Long storeId, OrderStatus status);
}
