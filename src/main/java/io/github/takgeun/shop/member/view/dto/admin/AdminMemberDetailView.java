package io.github.takgeun.shop.member.view.dto.admin;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class AdminMemberDetailView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final MemberStatus status;
    private final String joinDate;          // View DTO는 String 타입이 더 좋음. 포맷 로직을 하는 건 뷰 역할이 아니기 때문
    private final String lastLogin;
    private final int totalOrders;
    private final int totalSpent;
    private final String lastOrderDate;
    private final List<AdminMemberOrderItemView> recentOrders;

    private AdminMemberDetailView(
            Long id,
            String name,
            String email,
            String phone,
            MemberStatus status,
            String joinDate,
            String lastLogin,
            int totalOrders,
            int totalSpent,
            String lastOrderDate,
            List<AdminMemberOrderItemView> recentOrders
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.joinDate = joinDate;
        this.lastLogin = lastLogin;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.lastOrderDate = lastOrderDate;
        this.recentOrders = recentOrders;
    }

    public static AdminMemberDetailView of(Member member,
                                           List<AdminMemberOrderItemView> recentOrders,
                                           int totalOrders,
                                           int totalSpent) {

        String joinDate = member.getCreatedAt() != null
                ? member.getCreatedAt().format(DATE_FORMAT)
                : "-";
        String lastLogin = member.getLastLoginAt() != null
                ? member.getLastLoginAt().format(DATE_TIME_FORMAT)
                : "-";
        String lastOrderDate = recentOrders.isEmpty()
                ? "-"
                : recentOrders.get(0).getOrderDate();

        return new AdminMemberDetailView(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getStatus(),
                joinDate,
                lastLogin,
                totalOrders,
                totalSpent,
                lastOrderDate,
                recentOrders
        );
    }
}
