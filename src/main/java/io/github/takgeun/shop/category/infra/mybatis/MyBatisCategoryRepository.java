package io.github.takgeun.shop.category.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Profile("mybatis")
@RequiredArgsConstructor
public class MyBatisCategoryRepository implements CategoryRepository {

    private final CategoryMapper categoryMapper;

    @Override
    public Category save(Category category) {
        int affectedRows;

        if(category.getId() == null) {
            affectedRows = categoryMapper.insert(category);
            if(affectedRows != 1) {
                throw new IllegalStateException("카테고리 저장에 실패했습니다.");
            }
        } else {
            affectedRows = categoryMapper.update(category);
            if(affectedRows != 1) {
                throw new IllegalStateException("카테고리 수정에 실패했습니다. id=" + category.getId());
            }
        }
        return category;
    }

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(categoryMapper.findById(id));
    }

    @Override
    public List<Category> findAllAdmin() {
        return categoryMapper.findAll();
    }

    @Override
    public List<Category> findAllPublic() {
        List<Category> allCategories = categoryMapper.findAll();

        Map<Long, List<Category>> childrenByParentId = new LinkedHashMap<>();   // 입력 순서 유지 -> 트리 출력 시 순서 깨지지 않음.
        for (Category category : allCategories) {
            Long parentId = category.getParentId();
            childrenByParentId
                    .computeIfAbsent(parentId, k -> new ArrayList<>())  // parentId 키가 없으면 새 리스트 만들고, 있으면 기존 리스트 가져오기
                    .add(category);
        }

        List<Category> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        // 루트부터 DFS (현재 노드가 public 인 것만 결과에 포함시키기)
        List<Category> roots = childrenByParentId.getOrDefault(null, List.of());    // parentId가 null인 카테고리 가져오고 없으면 빈 리스트
        for (Category root : roots) {
            dfsAppendPublicChain(root, childrenByParentId, visited, result);
        }

        return result;
    }

    @Override
    public void deleteById(Long id) {
        int affectedRows = categoryMapper.deleteById(id);
        if(affectedRows != 1) {
            throw new IllegalStateException("카테고리 삭제에 실패했습니다. id=" + id);
        }
    }

    @Override
    public boolean existsByParentId(Long parentId) {
        if(parentId == null) {
            return false;
        }
        return categoryMapper.existsByParentId(parentId);
    }

    @Override
    public boolean existsByNameKey(String nameKey) {
        if(nameKey == null || nameKey.isEmpty()) {
            return false;
        }
        return categoryMapper.existsByNameKey(nameKey);
    }

    @Override
    public boolean existsBySlug(String slug) {
        if(slug == null || slug.isEmpty()) {
            return false;
        }
        return categoryMapper.existsBySlug(slug);
    }

    @Override
    public boolean existsByNameKeyExceptId(String nameKey, Long excludeId) {
        // 수정 상황일 때 중복 체크함.
        // 수정할 때 기존에 존재하는 nameKey로 업데이트가 되면 안되니까 자기 자신을 제외한 중복이 있는지 체크
        if(nameKey == null || nameKey.isEmpty()) {
            return false;
        }
        if(excludeId == null) {
            return existsByNameKey(nameKey);
        }
        return categoryMapper.existsByNameKeyExceptId(nameKey, excludeId);
    }

    @Override
    public boolean existsBySlugExceptId(String slug, Long excludeId) {
        if(slug == null || slug.isEmpty()) {
            return false;
        }
        if(excludeId == null) {
            return existsBySlug(slug);
        }
        return categoryMapper.existsBySlugExceptId(slug, excludeId);
    }

    private void dfsAppendPublicChain(Category node,
                                      Map<Long, List<Category>> childrenByParentId,
                                      Set<Long> visited,
                                      List<Category> out) {

        if(node == null) {
            return;
        }

        Long id = node.getId();
        if(id == null) {        // insert 하기 전 category 객체가 만들어져서 들어오는 것 방지
            return;
        }

        // 순환 참조/중복 방지 (visited가 Set 구조니까 이미 방문한 노드면 add 처리 시 false 반환할 것. 즉 해당 조건문은 true가 됨.)
        // A -> B -> C -> A 구조로 잘못 만들었을 때 A -> B -> C -> A 순으로 방문할 때 이미 visited에 있게 되므로 return하게 된다.
        // 무한 재귀 방지용
        if (!visited.add(id)) {
            return;
        }

        // 부모가 공개가 아니면 이 노드와 자식 노드 모두 안 보이게끔
        if(!node.isPublicVisible()) {
            return; // out.add 안함 + 자식 탐색도 안함
        }

        // 부모가 공개면 결과에 포함하기
        out.add(node);

        // 부모가 공개일 때만 자식 탐색하기
        List<Category> children = childrenByParentId.get(id);
        if(children == null) {
            return; // 자식카테고리들 없으면 패스
        }

        // 현재 노드와 인접한 자식 노드들 전부 DFS 순회
        for (Category child : children) {
            dfsAppendPublicChain(child, childrenByParentId, visited, out);
        }
    }
}
