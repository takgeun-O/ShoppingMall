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

        // id 기준 Map 변환
        // id -> CategoryResponse 형태의 Map으로 변환하기
        Map<Long, CategoryResponse> byId = all.stream()
                .collect(Collectors.toMap(CategoryResponse::getId, c -> c));

        // parentId -> children
        Map<Long, List<CategoryResponse>> childrenByParent = new HashMap<>();
        for (CategoryResponse c : all) {
            childrenByParent
                    .computeIfAbsent(c.getParentId(), k-> new ArrayList<>())
                    .add(c);
        }

        // 정렬 규칙 : 이름 오름차순
        childrenByParent.values().forEach(list ->
                list.sort(Comparator.comparing(CategoryResponse::getName)));

        // 선택한 카테고리가 없으면 전체 루트 모두 반환하기
        if(selectedCategoryId == null) {
            List<CategoryResponse> roots = childrenByParent.getOrDefault(null, List.of());
            return roots.stream()
                    .map(root -> buildNode(root, childrenByParent))
                    .toList();
        }

        // 선택한 카테고리가 있으면 선택 카테고리의 루트를 찾아 그 루트만 반환하기
        CategoryResponse selected = byId.get(selectedCategoryId);
        if(selected == null) {
            // 선택한 카테고리가 없으면 fallback: 전체 루트
            List<CategoryResponse> roots = childrenByParent.getOrDefault(null, List.of());
            return roots.stream()
                    .map(root -> buildNode(root, childrenByParent))
                    .toList();
        }

        CategoryResponse root = findRoot(selected, byId);
        return List.of(buildNode(root, childrenByParent));
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
        while(cur.getParentId() != null) {
            CategoryResponse parent = byId.get(cur.getParentId());
            if(parent == null) break;
            cur = parent;
        }
        return cur;
    }
}
