package io.github.takgeun.shop.member.view.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyPageOrderSummaryView {
    private final int paidCount;
    private final int readyCount;
    private final int shippingCount;
    private final int completedCount;

    public static MyPageOrderSummaryView stub() {
        return new MyPageOrderSummaryView(2, 1, 0, 12);
    }
}
