package io.github.takgeun.shop.category.view.dto.admin;

import lombok.Getter;

import java.util.List;

/**
 * 컨트롤러에서 한 번에 받기 좋게 묶기
 */
@Getter
public class AdminCategoryPageView {

    private final List<AdminCategoryItemView> categories;
    private final AdminCategorySummaryView summary;

    private AdminCategoryPageView(List<AdminCategoryItemView> categories, AdminCategorySummaryView summary) {
        this.categories = categories;
        this.summary = summary;
    }

    public static AdminCategoryPageView of(
            List<AdminCategoryItemView> categories,
            AdminCategorySummaryView summary
    ) {
        return new AdminCategoryPageView(categories, summary);
    }
}
