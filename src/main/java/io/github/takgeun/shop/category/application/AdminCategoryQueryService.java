package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.application.result.*;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 관리자 화면 조회/조립 전용
 * - 카테고리 트리 조회
 * - 부모/자식 구조 조립
 * - summary 계산
 * - 생성폼용 부모 후보 목록 조회
 * - 관리자 화면용 DTO 반환
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryQueryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * 관리자 카테고리 관리 페이지 조회
     */
    public AdminCategoryPageResult getCategoryPage() {
        List<AdminCategoryItemResult> categories = getCategoryTree();

        AdminCategorySummaryResult summary = buildSummary(categories);

        return AdminCategoryPageResult.of(categories, summary);
    }

    /**
     * 관리자 카테고리 수정 화면 조회
     */
    public AdminCategoryEditResult getEditResult(Long categoryId) {
        Category category = getCategoryOrThrow(categoryId);

        String parentName = findParentName(category.getParentId());

        return AdminCategoryEditResult.of(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParentId(),
                parentName
        );
    }

    /**
     * 관리자 폼에서 사용할 카테고리 선택 목록 조회
     */
    public List<CategoryOptionResult> getCategoryOptions() {
        return categoryRepository.findAllAdmin().stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(category -> CategoryOptionResult.of(
                        category.getId(),
                        category.getName()
                ))
                .toList();
    }

    /**
     * 관리자 카테고리 트리 조립
     */
    private List<AdminCategoryItemResult> getCategoryTree() {
        List<Category> categories = categoryRepository.findAllAdmin();

        // 카테고리 id로 View 객체를 빠르게 찾기 위한 맵
        Map<Long, AdminCategoryItemResult> viewMap = new LinkedHashMap<>();
        List<AdminCategoryItemResult> roots = new ArrayList<>();

        // viewMap 완성
        for (Category category : categories) {
            viewMap.put(
                    category.getId(),
                    AdminCategoryItemResult.of(
                            category.getId(),
                            category.getName(),
                            toSlug(category.getName()),
                            productRepository.countAdminByCategoryId(category.getId()),
                            category.getParentId()
                    )
            );
        }

        // 카테고리 순환하면서 루트 카테고리는 roots에 저장하고
        // 그 외 카테고리들은 부모자식 연결시키기
        for (Category category : categories) {
            AdminCategoryItemResult current = viewMap.get(category.getId());

            if (category.getParentId() == null) {
                roots.add(current);
                continue;
            }

            AdminCategoryItemResult parent = viewMap.get(category.getParentId());

            if (parent == null) {
                throw new IllegalStateException("카테고리의 부모 참조가 유효하지 않습니다.");
            }

            parent.addChild(current);
        }

        return roots;
    }

    /**
     * 관리자 카테고리 요약 정보 계산
     */
    private AdminCategorySummaryResult buildSummary(List<AdminCategoryItemResult> categories) {
        if (categories == null || categories.isEmpty()) {
            return AdminCategorySummaryResult.empty();
        }

        int totalCategoryCount = categories.size();

        int totalSubcategoryCount = categories.stream()
                .mapToInt(this::countCategoriesRecursively)
                .sum();

        int totalProductCount = categories.stream()
                .mapToInt(this::sumProductCountRecursively)
                .sum();

        return AdminCategorySummaryResult.of(
                totalCategoryCount,
                totalSubcategoryCount,
                totalProductCount
        );
    }

    /**
     * 현재 노드와 모든 하위 카테고리 수 계산
     */
    private int countCategoriesRecursively(AdminCategoryItemResult category) {
        int count = 1;

        for (AdminCategoryItemResult child : category.getChildren()) {
            count += countCategoriesRecursively(child);
        }

        return count;
    }

    /**
     * 현재 노드와 모든 하위 카테고리의 상품 수 계산
     */
    private int sumProductCountRecursively(AdminCategoryItemResult category) {
        int total = category.getProductCount();
        for (AdminCategoryItemResult child : category.getChildren()) {
            total += sumProductCountRecursively(child);
        }
        return total;
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private String findParentName(Long parentId) {
        if(parentId == null) {
            return null;
        }

        return categoryRepository.findById(parentId)
                .map(Category::getName)
                .orElseThrow(() -> new IllegalStateException("카테고리의 부모 참조가 유효하지 않습니다."));
    }

    private String toSlug(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .toLowerCase()
                .replace(" ", "-");
    }
}
