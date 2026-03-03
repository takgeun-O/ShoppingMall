package io.github.takgeun.shop.member.view.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class MyPageOrderSummaryView {
    private final int orderedCount;
    private final int paidCount;
    private final int readyCount;
    private final int shippingCount;
    private final int completedCount;
    private final int canceledCount;

    public MyPageOrderSummaryView(int orderedCount, int paidCount, int readyCount,
                                  int shippingCount, int completedCount, int canceledCount) {
        this.orderedCount = orderedCount;
        this.paidCount = paidCount;
        this.readyCount = readyCount;
        this.shippingCount = shippingCount;
        this.completedCount = completedCount;
        this.canceledCount = canceledCount;
    }

    public static MyPageOrderSummaryView of(int orderedCount, int paidCount, int readyCount,
                                            int shippingCount, int completedCount, int canceledCount) {
        return new MyPageOrderSummaryView(
                Math.max(orderedCount, 0),
                Math.max(paidCount, 0),
                Math.max(readyCount, 0),
                Math.max(shippingCount, 0),
                Math.max(completedCount, 0),
                Math.max(canceledCount, 0)
        );
    }
}
