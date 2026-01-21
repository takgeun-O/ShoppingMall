package io.github.takgeun.shop.product.infra;

import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class MemoryProductRepository implements ProductRepository {

    // 저장 순서 유지하기 위해 LinkedHashMap 사용 (findAll 안정적)
    private final Map<Long, Product> store = new LinkedHashMap<>();
    private long sequence = 0L;     // 동시성 문제는 추후 해결할 것.

    @Override
    public Product save(Product product) {
        if(product.getId() == null) {
            long id = ++sequence;
            product.assignId(id);
            store.put(id, product);
            return product;
        }

        // 수정할 때 기존 id로 덮어쓰기.
        // 수정은 Service에서 findById(id)를 활용해서 엔티티를 가져온 후
        // 엔티티의 의미 있는 메서드로 상태를 바꾼다. (changeName 등등)
        // 이후 Service에서 repository.save(entity) 를 호출해서 저장한다.
        // 우선 메모리 저장이니까 Map에 덮어쓰기.
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));      // 값이 있으면 Optional<Category> 없으면 Optional.empty()
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Product> findAllByCategoryId(Long categoryId) {
        return store.values().stream()
                .filter(p -> p.getCategoryId().equals(categoryId))
                .collect(Collectors.toList());
        // [컬렉션] → stream() → 중간연산(filter) → 최종연산(collect)
        // collect :스트림을 다시 컬렉션으로 모아라.
        // Stream<Product> 필터링된 상태를 collect 사용해서 List<Product>로 수거(collect)
        // Collectors.toList() : 비어 있는 List 생성 -> 스트림의 각 요소(Product)를 List에 하나씩 add -> 최종 List 반환
        // stream은 그 자체로 반환할 수 없기 때문에 반드시 최종 연산으로 끝내서 반환해야 함.
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return store.values().stream()
                .anyMatch(p -> categoryId != null && categoryId.equals(p.getCategoryId()));
    }
}
