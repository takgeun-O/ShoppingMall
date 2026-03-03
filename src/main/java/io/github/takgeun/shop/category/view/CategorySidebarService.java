package io.github.takgeun.shop.category.view;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.view.dto.CategoryNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategorySidebarService {

    private final CategoryService categoryService;

    public CategorySidebarService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 선택된 카테고리가 없으면
    // parentId == null 인 루트들을 전부 찾아서
    // 각각의 서브트리를 만들어 리스트로 내려주기.

    // 선택된 카테고리가 있으면
    // 그 카테고리의 루트를 찾아서
    // 그 루트의 서브트리 하나만 내려주기
    public List<CategoryNode> buildSidebarTrees(Long selectedCategoryId) {
        List<CategoryResponse> all = categoryService.getAllPublicCategories();

        if (all == null || all.isEmpty()) return List.of();

//        // id 기준 Map 변환
//        // id -> CategoryResponse 형태의 Map으로 변환하기
//        Map<Long, CategoryResponse> byId = all.stream()
//                .collect(Collectors.toMap(CategoryResponse::getProductId, c -> c));

        // parentId -> children
        Map<Long, List<CategoryResponse>> childrenByParent = new HashMap<>();
        for (CategoryResponse c : all) {
            childrenByParent
                    .computeIfAbsent(c.getParentId(), k -> new ArrayList<>())
                    .add(c);
        }

        // 정렬 규칙 : 이름 오름차순
        childrenByParent.values().forEach(list ->
                list.sort(Comparator.comparing(CategoryResponse::getName)));

        // 선택 카테고리와 무관하게 전체 루트 항상 반환
        List<CategoryResponse> roots = childrenByParent.getOrDefault(null, List.of());
        return roots.stream()
                .map(root -> buildNode(root, childrenByParent))
                .toList();
    }

    // DFS로 선택 카테고리 + 모든 하위 카테고리를 모으기
    public Set<Long> buildSubtreeIds(Long selectedCategoryId, Map<Long, List<CategoryResponse>> childrenByParent) {
        if(selectedCategoryId == null) return Set.of();

        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(selectedCategoryId);

        while(!stack.isEmpty()) {
            Long id = stack.pop();
            if(!ids.add(id)) continue;  // 중복 방지

            for (CategoryResponse child : childrenByParent.getOrDefault(id, List.of())) {
                stack.push(child.getId());
            }
        }
        return ids;
    }

    // (현재 처리 중인 카테고리, parentId -> 자식 목록 Map)
    private CategoryNode buildNode(CategoryResponse c, Map<Long, List<CategoryResponse>> childrenByParent) {
        List<CategoryNode> children = childrenByParent
                .getOrDefault(c.getId(), List.of())     // 현재 처리 중인 카테고리의 자식 목록 조회
                .stream()
                .map(child -> buildNode(child, childrenByParent))// 각 자식을 다시 buildNode()로 호출 (자식 -> 그 자식의 자식 -> ...)
                .toList();
        return new CategoryNode(c.getId(), c.getName(), children);
    }

    private CategoryResponse findRoot(CategoryResponse c, Map<Long, CategoryResponse> byId) {
        CategoryResponse cur = c;
        while (cur.getParentId() != null) {
            CategoryResponse parent = byId.get(cur.getParentId());
            if (parent == null) break;
            cur = parent;
        }
        return cur;
    }

    //
    public Set<Long> buildOpenIds(Long selectedCategoryId, List<CategoryResponse> all) {
        if (selectedCategoryId == null) return Set.of();        // 정책 : 선택 없으면 모두 접기
        if (all == null || all.isEmpty()) return Set.of();      // 카테고리가 전부 없거나 비활성화면 바로 끝내기

        /*
            [ {id:1}, {id:2}, {id:3} ] 형태의 리스트 all을 아래와 같이 변환
            { 1 -> 객체1,
              2 -> 객체2,
              3 -> 객체3 }
         */
//        Map<Long, CategoryResponse> byId = new HashMap<>();
//        for (CategoryResponse c : all) {
//            byId.put(c.getProductId(), c);
//        }

        Map<Long, CategoryResponse> byId = all.stream()
                .collect(Collectors.toMap(CategoryResponse::getId, c -> c));

        Set<Long> open = new HashSet<>();
        CategoryResponse cur = byId.get(selectedCategoryId);
        while (cur != null) {
            // (선택된 카테고리 노드 -> 부모 카테고리 노드 -> 부모의 부모 카테고리 노드 -> ...)를 open할 대상으로 추가
            open.add(cur.getId());
            Long pid = cur.getParentId();
            if (pid == null) break;     // 루트 도달
            cur = byId.get(pid);
        }
        return open;
    }

    // 트리 렌더링용 : parentId -> children 목록 맵
    // categoryTree.html에서 사용 예정
    public Map<Long, List<CategoryResponse>> groupByParent(List<CategoryResponse> all) {
        if(all == null || all.isEmpty()) return Map.of();

        Map<Long, List<CategoryResponse>> childrenByParent = new LinkedHashMap<>();
        for (CategoryResponse c : all) {
            Long parentId = c.getParentId();        // null 이면 루트
            childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>())
                    .add(c);
        }

        List<CategoryResponse> roots = childrenByParent.get(null);
        if(roots != null) {
            roots.sort(Comparator.comparing(CategoryResponse::getName));
        }

        return childrenByParent;
    }
}
