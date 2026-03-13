package io.github.takgeun.shop.member.view.dto.admin;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.domain.Order;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 데이터 표시용
 */
@Getter
public class AdminMemberEditView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final Long id;
    private final String name;
    private final String email;
    private final String phone;
    private final String joinDate;
    private final String lastLogin;
    private final MemberStatus status;
    private final int totalOrders;
    private final int totalSpent;
    private final String lastOrderDate;

    private AdminMemberEditView(Long id,
                               String name,
                               String email,
                               String phone,
                               String joinDate,
                               String lastLogin,
                               MemberStatus status,
                               int totalOrders,
                               int totalSpent,
                               String lastOrderDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joinDate = joinDate;
        this.lastLogin = lastLogin;
        this.status = status;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.lastOrderDate = lastOrderDate;
    }

    public static AdminMemberEditView of(
            Long id,
            String name,
            String email,
            String phone,
            String joinDate,
            String lastLogin,
            MemberStatus status,
            int totalOrders,
            int totalSpent,
            String lastOrderDate
    ) {
        return new AdminMemberEditView(
                id,
                name,
                email,
                phone,
                joinDate,
                lastLogin,
                status,
                totalOrders,
                totalSpent,
                lastOrderDate
        );
    }

    public static AdminMemberEditView from(Member member, List<Order> orders) {
        int totalOrders = orders.size();
        int totalSpent = orders.stream()
                .mapToInt(Order::getTotalPrice)
                .sum();

        String formattedJoinDate = member.getCreatedAt() != null
                ? member.getCreatedAt().format(DATE_FORMAT)
                : "-";
        String formattedLastLogin = member.getLastLoginAt() != null
                ? member.getLastLoginAt().format(DATE_TIME_FORMAT)
                : "-";
        String lastOrderDate = orders.stream()
                .max(Comparator.comparing(Order::getOrderedAt))
                .map(order -> order.getOrderedAt().format(DATE_FORMAT))
                .orElse("-");

        return new AdminMemberEditView(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                formattedJoinDate,
                formattedLastLogin,
                member.getStatus(),
                totalOrders,
                totalSpent,
                lastOrderDate
        );
    }
}
