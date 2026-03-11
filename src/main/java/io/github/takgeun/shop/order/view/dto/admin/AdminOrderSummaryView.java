package io.github.takgeun.shop.order.view.dto.admin;

import lombok.Getter;

@Getter
public class AdminOrderSummaryView {

    private final int totalCount;
    private final int orderedCount;
    private final int paymentCompletedCount;
    private final int preparingCount;
    private final int shippingCount;
    private final int deliveredCount;
    private final int canceledCount;

    private AdminOrderSummaryView(int totalCount,
                                 int orderedCount,
                                 int paymentCompletedCount,
                                 int preparingCount,
                                 int shippingCount,
                                 int deliveredCount,
                                 int canceledCount) {
        this.totalCount = totalCount;
        this.orderedCount = orderedCount;
        this.paymentCompletedCount = paymentCompletedCount;
        this.preparingCount = preparingCount;
        this.shippingCount = shippingCount;
        this.deliveredCount = deliveredCount;
        this.canceledCount = canceledCount;
    }

    public static AdminOrderSummaryView of(
            int totalCount,
            int orderedCount,
            int paymentCompletedCount,
            int preparingCount,
            int shippingCount,
            int deliveredCount,
            int canceledCount
    ) {
        return new AdminOrderSummaryView(
                totalCount,
                orderedCount,
                paymentCompletedCount,
                preparingCount,
                shippingCount,
                deliveredCount,
                canceledCount);
    }
}
