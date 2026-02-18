package io.github.takgeun.shop.category.infra;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MemoryCategoryRepository implements CategoryRepository {

    // 저장 순서 유지하기 위해 LinkedHashMap 사용 (findAllAdmin 안정적)
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
    public List<Category> findAllAdmin() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Category> findAllPublic() {
//        return store.values().stream()
//                .filter(Category::isPublicVisible)
//                .toList();

        // 전체 카테고리로 parentId -> children 인덱스 만들기 (등록 순서 유지)
        Map<Long, List<Category>> childrenByParentId = new LinkedHashMap<>();
        for (Category c : store.values()) {
            Long parentId = c.getParentId();    // null 이면 루트
            childrenByParentId
                    .computeIfAbsent(parentId, k -> new ArrayList<>())
                    .add(c);
            // null -> [전자, 의류]
            // 1 -> [노트북, 휴대폰, 상의]
            // parentId 키가 존재하면 거기에 맞게 맵핑된 List<Category> 반환하고,
            // parentId 키가 없으면 new ArrayList 샹송해서 Map에 저장한 뒤 그 리스트 반환
            // 그리고 그 반환된 리스트에 .add(c) 하기
        }

        // 루트부터 DFS : 현재 노드가 public 일 때만 결과에 포함 + 자식으로 내려가기
        List<Category> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        List<Category> roots = childrenByParentId.getOrDefault(null, List.of());
        for (Category root : roots) {
            dfsAppendPublicChain(root, childrenByParentId, visited, result);
        }

        return result;
    }

    private void dfsAppendPublicChain(Category node, Map<Long, List<Category>> childrenByParentId, Set<Long> visited, List<Category> out) {
        if(node == null) return;

        Long id = node.getId();
        if(id == null) return;

        // 순환 참조/중복 방지
        if(!visited.add(id)) return;

        // 부모가 공개가 아니면 이 노드도 숨기고 자식도 전부 숨기기
        if(!node.isPublicVisible()) {
            return; // out.add 안함 + 자식 탐색도 안함
        }

        // 부모가 공개면 결과에 포함하기
        out.add(node);
        // 부모가 공개일 때만 자식 탐색하기
        List<Category> children = childrenByParentId.get(id);
        if(children == null) return;

        for (Category child : children) {
            dfsAppendPublicChain(child, childrenByParentId, visited, out);
        }
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
