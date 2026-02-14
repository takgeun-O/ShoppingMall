package io.github.takgeun.shop.category.infra;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MemoryCategoryRepository implements CategoryRepository {

    // 저장 순서 유지하기 위해 LinkedHashMap 사용 (findAll 안정적)
    private final Map<Long, Category> store = new LinkedHashMap<>();
    private long sequence = 0L;     // 추후 동시성 문제 해결할 것.

    @Override
    public Category save(Category category) {
        if(category.getId() == null) {
            long id = ++sequence;
            category.assignId(id);
        }

        // 신규, 수정 모두 덮어쓰기
        store.put(category.getId(), category);
        return category;
    }

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(store.get(id));      // 값이 있으면 Optional<Category> 없으면 Optional.empty()
    }

    @Override
    public List<Category> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        // id 존재 여부 판단은 Service 책임
        store.remove(id);
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        if(parentId == null) return false;

        return store.values().stream()
                .anyMatch(c ->  parentId.equals(c.getParentId()));
    }

    /**
     * nameKey는 서비스에서 Category.normalizeKey(name)로 만들어서 넘겨주기.
     * repository에서는 추가 정규화 없이 그대로 비교만
     */
    @Override
    public boolean existsByNameKey(String nameKey) {
        if(nameKey == null || nameKey.isEmpty()) return false;

        return store.values().stream()
                .anyMatch(c -> nameKey.equals(c.getNameKey()));
    }

    @Override
    public boolean existsByNameKeyExceptId(String nameKey, Long excludeId) {
        if(nameKey == null || nameKey.isEmpty()) return false;

        return store.values().stream()
                .anyMatch(c -> c.getId() != null
                        && !c.getId().equals(excludeId)
                        && nameKey.equals(c.getNameKey()));
    }
}
