package io.github.takgeun.shop.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    boolean existsById(Long id);        // 주문이 존재하는지 체크

    List<Order> findAllByMemberId(Long memberId);
}
