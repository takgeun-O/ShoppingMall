package io.github.takgeun.shop.product.infra;

import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class MemoryProductRepository implements ProductRepository {

    // 저장 순서 유지하기 위해 LinkedHashMap 사용 (findAllAdmin 안정적)
    private final Map<Long, Product> store = new LinkedHashMap<>();
    private long sequence = 0L;     // 동시성 문제는 추후 해결할 것.

    @Override
    public Product save(Product product) {
        if(product.getId() == null) {
            long id = ++sequence;
            product.assignId(id);
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
                .filter(p -> Objects.equals(p.getCategoryId(), categoryId))
                .toList();
    }

    @Override
    public List<Product> findAllByCategoryIdIn(List<Long> categoryIds) {
        Set<Long> set = new HashSet<>(categoryIds);

        return store.values().stream()
                .filter(p -> set.contains(p.getCategoryId()))
                .toList();
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return store.values().stream()
                .anyMatch(p -> Objects.equals(p.getCategoryId(), categoryId));
    }

    @Override
    public List<Product> findAllPublicByCategoryId(Long categoryId) {
        return store.values().stream()
                .filter(p -> Objects.equals(p.getCategoryId(), categoryId))
                .filter(Product::isPublicVisible)
                .toList();
    }

    @Override
    public List<Product> findAllPublicByCategoryIds(Set<Long> categoryIds) {

        if(categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }

        return store.values().stream()
                .filter(p -> categoryIds.contains(p.getCategoryId()))
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE || p.getStatus() == ProductStatus.SOLD_OUT)
                .toList();
    }

    @Override
    public List<Product> findAllAdminByCategoryIds(Set<Long> categoryIds) {

        if(categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }

        return store.values().stream()
                .filter(p -> categoryIds.contains(p.getCategoryId()))
                .toList();
    }

    @Override
    public List<Product> findAllPublic() {
        return store.values().stream()
                .filter(Product::isPublicVisible)
                .toList();
    }

    @Override
    public List<Product> findAllAdmin() {
        return store.values().stream()
                .toList();
    }

    @Override
    public List<Product> findAllAdminByCategoryId(Long categoryId) {
        return store.values().stream()
                .filter(p -> Objects.equals(p.getCategoryId(), categoryId))
                .toList();
    }
}
