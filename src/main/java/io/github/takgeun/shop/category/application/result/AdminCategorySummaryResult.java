package io.github.takgeun.shop.category.application.result;

import lombok.Getter;

// 화면에 필요한 값만 담는 읽기 전용 객체
@Getter
public class AdminCategorySummaryResult {

    private final int totalCategoryCount;
    private final int totalSubcategoryCount;
    private final int totalProductCount;

    private AdminCategorySummaryResult(int totalCategoryCount, int totalSubcategoryCount, int totalProductCount) {
        this.totalCategoryCount = totalCategoryCount;
        this.totalSubcategoryCount = totalSubcategoryCount;
        this.totalProductCount = totalProductCount;
    }

    public static AdminCategorySummaryResult of(
            int totalCategoryCount,
            int totalSubcategoryCount,
            int totalProductCount
    ) {
        return new AdminCategorySummaryResult(
                totalCategoryCount,
                totalSubcategoryCount,
                totalProductCount
        );
    }

    public static AdminCategorySummaryResult empty() {
        return new AdminCategorySummaryResult(0, 0, 0);
    }
}
