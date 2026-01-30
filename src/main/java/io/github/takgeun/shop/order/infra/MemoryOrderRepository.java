package io.github.takgeun.shop.order.infra;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MemoryOrderRepository implements OrderRepository {

    private final ConcurrentHashMap<Long, Order> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Order save(Order order) {
        if(order.getId() == null) {
            long id = sequence.incrementAndGet();
            order.assignId(id);
        }
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));  // 값이 있으면 Optional<Category> 없으면 Optional.empty()
    }

    @Override
    public boolean existsById(Long id) {
        if(id == null) return false;
        return store.containsKey(id);
    }

    @Override
    public List<Order> findAllByMemberId(Long memberId) {
        if(memberId == null) return List.of();

        List<Order> result = new ArrayList<>();
        for (Order order : store.values()) {
            if(memberId.equals(order.getMemberId())) {
                result.add(order);
            }
        }

        // 최근 주문 먼저 보이게 하기. (내림차순)
        result.sort(Comparator.comparing(Order::getOrderedAt).reversed());

        return result;
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    // 테스트용
    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
