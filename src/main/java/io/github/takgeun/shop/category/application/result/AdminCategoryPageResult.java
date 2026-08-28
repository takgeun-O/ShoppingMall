package io.github.takgeun.shop.category.application.result;

import lombok.Getter;

import java.util.List;

/**
 * 컨트롤러에서 한 번에 받기 좋게 묶기
 */
@Getter
public class AdminCategoryPageResult {

    private final List<AdminCategoryItemResult> categories;
    private final AdminCategorySummaryResult summary;

    private AdminCategoryPageResult(List<AdminCategoryItemResult> categories, AdminCategorySummaryResult summary) {
        this.categories = categories;
        this.summary = summary;
    }

    public static AdminCategoryPageResult of(
            List<AdminCategoryItemResult> categories,
            AdminCategorySummaryResult summary
    ) {
        return new AdminCategoryPageResult(categories, summary);
    }
}
