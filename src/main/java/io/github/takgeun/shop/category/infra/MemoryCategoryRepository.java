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
            store.put(id, category);
            return category;
        }

        // 수정할 때 기존 id로 덮어쓰기.
        // 수정은 Service에서 findById(id)를 활용해서 엔티티를 가져온 후
        // 엔티티의 의미 있는 메서드로 상태를 바꾼다. (changeName, changeParent 등등)
        // 이후 Service에서 repository.save(entity) 를 호출해서 저장한다.
        // 우선 메모리 저장이니까 Map에 덮어쓰기.
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
    public boolean existsByName(String name) {
        // trim + 공백 방지 + 대소문자 무시
        if(name == null) {
            return false;
        }

        String normalized = name.trim();
        if(normalized.isEmpty()) {
            return false;
        }

        return store.values().stream()
                .anyMatch(c -> c.getName() != null && c.getName().trim().toLowerCase().equals(normalized));
    }
}
