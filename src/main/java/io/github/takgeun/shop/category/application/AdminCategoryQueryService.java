package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategoryItemView;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategoryPageView;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategorySummaryView;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class AdminCategoryQueryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public AdminCategoryPageView getCategoryPage() {
        List<AdminCategoryItemView> categories = getCategoryTree();
        AdminCategorySummaryView summary = buildSummary(categories);

        return AdminCategoryPageView.of(categories, summary);
    }

    private List<AdminCategoryItemView> getCategoryTree() {
        List<Category> all = categoryRepository.findAllAdmin();

        // 카테고리 id로 View 객체를 빠르게 찾기 위한 맵
        Map<Long, AdminCategoryItemView> viewMap = new LinkedHashMap<>();
        List<AdminCategoryItemView> roots = new ArrayList<>();

        // viewMap 완성
        for (Category category : all) {
            viewMap.put(
                    category.getId(),
                    AdminCategoryItemView.of(
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
        for (Category category : all) {
            AdminCategoryItemView current = viewMap.get(category.getId());

            if(category.getParentId() == null) {
                roots.add(current);
                continue;
            }

            AdminCategoryItemView parent = viewMap.get(category.getParentId());
            if(parent == null) {
                throw new NotFoundException("부모 카테고리를 찾을 수 없습니다. id=" + category.getParentId());
            }
            parent.addChild(current);
        }

        return roots;
    }

    private AdminCategorySummaryView buildSummary(List<AdminCategoryItemView> categories) {
        if(categories == null || categories.isEmpty()) {
            return AdminCategorySummaryView.empty();
        }

        int totalCategoryCount = categories.size();

        int totalSubcategoryCount = categories.stream()
                .mapToInt(category -> category.getChildren().size())
                .sum();

        int totalProductCount = categories.stream()
                .mapToInt(this::sumProductCountRecursively)
                .sum();

        return AdminCategorySummaryView.of(
                totalCategoryCount,
                totalSubcategoryCount,
                totalProductCount
        );
    }

    private int sumProductCountRecursively(AdminCategoryItemView category) {
        int total = category.getProductCount();
        for (AdminCategoryItemView child : category.getChildren()) {
            total += sumProductCountRecursively(child);
        }
        return total;
    }

    private String toSlug(String name) {
        if(name == null) {
            return "";
        }
        return name.trim()
                .toLowerCase()
                .replace(" ", "-");
    }
}
