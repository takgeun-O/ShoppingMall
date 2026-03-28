package io.github.takgeun.shop.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByRequestKey(String requestKey);

    List<Order> findAllByMemberId(Long memberId);

    int countByMemberId(Long memberId);

    List<Order> findAll();

    boolean existsByRequestKey(String requestKey);
}
