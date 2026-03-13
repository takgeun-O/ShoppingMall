package io.github.takgeun.shop.admin.view.dto;

import io.github.takgeun.shop.order.view.dto.admin.AdminOrderItemView;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminDashboardView {

    private final int totalOrderCount;
    private final int totalMemberCount;
    private final int totalProductCount;
    private final int totalRevenue;
    private final List<AdminOrderItemView> recentOrders;


    private AdminDashboardView(int totalOrderCount,
                               int totalMemberCount,
                               int totalProductCount,
                               int totalRevenue,
                               List<AdminOrderItemView> recentOrders
    ) {
        this.totalOrderCount = totalOrderCount;
        this.totalMemberCount = totalMemberCount;
        this.totalProductCount = totalProductCount;
        this.totalRevenue = totalRevenue;
        this.recentOrders = recentOrders;
    }

    public static AdminDashboardView of(
            int totalOrderCount,
            int totalMemberCount,
            int totalProductCount,
            int totalRevenue,
            List<AdminOrderItemView> recentOrders
    ) {
        return new AdminDashboardView(
                totalOrderCount,
                totalMemberCount,
                totalProductCount,
                totalRevenue,
                recentOrders
        );
    }
}
