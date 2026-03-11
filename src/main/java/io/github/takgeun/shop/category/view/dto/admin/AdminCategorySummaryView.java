package io.github.takgeun.shop.category.view.dto.admin;

import lombok.Getter;

// 화면에 필요한 값만 담는 읽기 전용 객체
@Getter
public class AdminCategorySummaryView {

    private final int totalCategoryCount;
    private final int totalSubcategoryCount;
    private final int totalProductCount;

    private AdminCategorySummaryView(int totalCategoryCount, int totalSubcategoryCount, int totalProductCount) {
        this.totalCategoryCount = totalCategoryCount;
        this.totalSubcategoryCount = totalSubcategoryCount;
        this.totalProductCount = totalProductCount;
    }

    public static AdminCategorySummaryView of(
            int totalCategoryCount,
            int totalSubcategoryCount,
            int totalProductCount
    ) {
        return new AdminCategorySummaryView(
                totalCategoryCount,
                totalSubcategoryCount,
                totalProductCount
        );
    }

    public static AdminCategorySummaryView empty() {
        return new AdminCategorySummaryView(0, 0, 0);
    }
}
