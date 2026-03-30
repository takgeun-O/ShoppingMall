package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mybatis")
@RequiredArgsConstructor
public class MybatisOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Order save(Order order) {
        if(order == null) {
            throw new IllegalArgumentException("order는 필수입니다.");
        }
        if(order.getId() == null) {
            return saveNew(order);
        }

        return update(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        if(id == null) {
            return Optional.empty();
        }

        Order order = orderMapper.findById(id);
        if(order == null) {
            return Optional.empty();
        }

        loadOrderItems(order);

        return Optional.of(order);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        if(orderNumber == null || orderNumber.isBlank()) {
            return Optional.empty();
        }

        Order order = orderMapper.findByOrderNumber(orderNumber);
        if(order == null) {
            return Optional.empty();
        }

        loadOrderItems(order);
        return Optional.of(order);
    }

    @Override
    public Optional<Order> findByRequestKey(String requestKey) {
        if(requestKey == null || requestKey.isBlank()) {
            return Optional.empty();
        }

        Order order = orderMapper.findByRequestKey(requestKey);
        if(order == null) {
            return Optional.empty();
        }

        loadOrderItems(order);
        return Optional.of(order);
    }

    @Override
    public boolean existsByRequestKey(String requestKey) {
        if(requestKey == null || requestKey.isBlank()) {
            return false;
        }
        return orderMapper.existsByRequestKey(requestKey);
    }

    @Override
    public List<Order> findAllByMemberId(Long memberId) {
        if(memberId == null) {
            return List.of();
        }

        List<Order> orders = orderMapper.findAllByMemberId(memberId);
        // Order 조회 -> 각 OrderItems 조회 -> Order 도메인에 OrderItems 붙이기
        attachOrderItems(orders);
        return orders;
    }

    @Override
    public int countByMemberId(Long memberId) {
        if(memberId == null) {
            return 0;
        }

        return orderMapper.countByMemberId(memberId);
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = orderMapper.findAll();
        attachOrderItems(orders);
        return orders;
    }




    private Order saveNew(Order order) {
        int affectedRows = orderMapper.insert(order);
        if(affectedRows != 1) {
            throw new IllegalStateException("주문 저장에 실패했습니다.");
        }
        if(order.getId() == null) {
            throw new IllegalStateException("주문 저장 후 id가 할당되지 않았습니다.");
        }

        List<OrderItem> orderItems = order.getOrderItems();
        if(orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem orderItem : orderItems) {
                orderItem.assignOrderId(order.getId());

                int itemAffectedRows = orderItemMapper.insert(orderItem);
                if(itemAffectedRows != 1) {
                    throw new IllegalStateException("주문 상품 저장에 실패했습니다.");
                }
            }
        }
        return order;
    }

    private Order update(Order order) {
        int affectedRows = orderMapper.update(order);
        if(affectedRows != 1) {
            throw new IllegalStateException("주문 수정에 실패했습니다. id=" + order.getId());
        }

        Long orderId = order.getId();

        orderItemMapper.deleteByOrderId(orderId);

        List<OrderItem> orderItems = order.getOrderItems();
        if(orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem orderItem : orderItems) {
                if(orderItem.getOrderId() == null) {
                    orderItem.assignOrderId(orderId);
                }

                int itemAffectedRows = orderItemMapper.insert(orderItem);
                if(itemAffectedRows != 1) {
                    throw new IllegalStateException("주문 상품 수정 저장에 실패했습니다. orderId=" + orderId);
                }
            }
        }
        return order;
    }

    private void loadOrderItems(Order order) {
        List<OrderItem> orderItems = orderItemMapper.findAllByOrderId(order.getId());
        order.replaceOrderItems(orderItems);
    }

    private void attachOrderItems(List<Order> orders) {
        for (Order order : orders) {
            loadOrderItems(order);
        }
    }
}
