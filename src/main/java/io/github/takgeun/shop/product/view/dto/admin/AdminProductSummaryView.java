package io.github.takgeun.shop.product.view.dto.admin;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.Getter;

import java.util.List;

// 화면에 필요한 값만 담는 읽기 전용 객체
@Getter
public class AdminProductSummaryView {

    private final int totalCount;
    private final int readyCount;
    private final int onSaleCount;
    private final int hiddenCount;
    private final int soldOutCount;
    private final int discontinuedCount;
    private final int lowStockCount;

    private AdminProductSummaryView(
            int totalCount,
            int readyCount,
            int onSaleCount,
            int hiddenCount,
            int soldOutCount,
            int discontinuedCount,
            int lowStockCount) {
        this.totalCount = totalCount;
        this.readyCount = readyCount;
        this.onSaleCount = onSaleCount;
        this.hiddenCount = hiddenCount;
        this.soldOutCount = soldOutCount;
        this.discontinuedCount = discontinuedCount;
        this.lowStockCount = lowStockCount;
    }

    public static AdminProductSummaryView of(List<AdminProductListItemView> products) {
        int totalCount = products.size();
        int readyCount = (int) products.stream()
                .filter(p -> p.getStatus() == ProductStatus.READY)
                .count();
        int onSaleCount = (int) products.stream()
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
                .count();
        int hiddenCount = (int) products.stream()
                .filter(p -> p.getStatus() == ProductStatus.HIDDEN)
                .count();
        int soldOutCount = (int) products.stream()
                .filter(p -> p.getStatus() == ProductStatus.SOLD_OUT)
                .count();
        int discontinuedCount = (int) products.stream()
                .filter(p -> p.getStatus() == ProductStatus.DISCONTINUED)
                .count();
        int lowStockCount = (int) products.stream()
                .filter(p -> p.getStock() > 0 && p.getStock() < 10)
                .count();

        return new AdminProductSummaryView(totalCount, readyCount, onSaleCount, hiddenCount, soldOutCount, discontinuedCount, lowStockCount);
    }
}
