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

    // 정렬 정책 한 곳에 모아두기 (유지보수에 좋음)
    private static final Comparator<Order> ORDERED_AT_DESC =
            Comparator.comparing(Order::getOrderedAt).reversed();

    @Override
    public Order save(Order order) {
        if (order == null) throw new IllegalArgumentException("order는 필수입니다.");

        if (order.getId() == null) {
            long id = sequence.incrementAndGet();
            order.assignId(id);
        }

        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return id != null && store.containsKey(id);
    }

    @Override
    public List<Order> findAllByMemberId(Long memberId) {
        if (memberId == null) {
            return List.of();
        }

        List<Order> result = new ArrayList<>();
        for (Order order : store.values()) {
            if (memberId.equals(order.getMemberId())) {
                result.add(order);
            }
        }

        // 최근 주문 먼저 보이게 하기. (내림차순)
        result.sort(ORDERED_AT_DESC);

        return result;
    }

    @Override
    public int countByMemberId(Long memberId) {
        if(memberId == null) {
            return 0;
        }

        int count = 0;
        for (Order order : store.values()) {
            if(memberId.equals(order.getMemberId())) {
                count++;
            }
        }

        return count;
    }

    @Override
    public List<Order> findAll() {
        List<Order> result = new ArrayList<>(store.values());
        result.sort(ORDERED_AT_DESC);
        return result;
    }

    // 테스트용
    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
