package io.github.takgeun.shop.order.infra.memory;

import io.github.takgeun.shop.global.error.ConflictException;
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
    private final ConcurrentHashMap<String, Long> requestKeyIndex = new ConcurrentHashMap<>();  //requestKey -> orderId
    private final AtomicLong sequence = new AtomicLong(0);

    // 정렬 정책 한 곳에 모아두기 (유지보수에 좋음)
    private static final Comparator<Order> ORDERED_AT_DESC =
            Comparator.comparing(Order::getOrderedAt).reversed();

    @Override
    public Order save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order는 필수입니다.");
        }

        String requestKey = order.getRequestKey();
        if(requestKey == null || requestKey.isBlank()) {
            throw new IllegalArgumentException("requestKey는 필수입니다.");
        }

        // 맵에 requestKey가 없으면 -1을 넣고, 이미 있으면 기존 값 반환
        // 처음 요청 시 맵에 requestKey 없음 -> -1 저장 -> existingOrderId==null 반환 -> 주문 생성
        // 같은 요청이 동시에 들어옴 -> 이미 맵에 있음 [requestKey, -1] -> 아무것도 안함 -> return -1 -> existingOrderId = -1
        // 이미 주문 생성 완료 후 -> 맵 [requestKey, 1023] -> putIfAbsent return -> existingOrderId = 1023
        Long existingOrderId = requestKeyIndex.putIfAbsent(requestKey, -1L);
        if(existingOrderId != null) {
            if(existingOrderId > 0) {
                Order existingOrder = store.get(existingOrderId);
                if(existingOrder != null) {
                    throw new ConflictException("이미 처리된 주문 요청입니다.");
                }
            }
            throw new ConflictException("이미 처리 중인 주문 요청입니다.");
        }

        try {
            if (order.getId() == null) {
                long id = sequence.incrementAndGet();
                order.assignId(id);
            }

            store.put(order.getId(), order);

            // 선점 상태(-1) -> 실제 orderId로 치환
            requestKeyIndex.put(requestKey, order.getId());

            return order;
        } catch (RuntimeException e) {
            // 저장 실패 시 선점 롤백
            requestKeyIndex.remove(requestKey);
            throw e;
        }

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

    @Override
    public Optional<Order> findByRequestKey(String requestKey) {
        if(requestKey == null || requestKey.isBlank()) {
            return Optional.empty();
        }

        Long orderId = requestKeyIndex.get(requestKey);
        if(orderId == null || orderId <= 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(store.get(orderId));
    }

    // 테스트용
    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
