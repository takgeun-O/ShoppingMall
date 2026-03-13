package io.github.takgeun.shop.member.view.dto.admin;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class AdminMemberItemView {

    private static final DateTimeFormatter JOIN_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final Long id;
    private final String memberCode;
    private final String name;
    private final String email;
    private final String phone;
    private final MemberRole role;
    private final MemberStatus status;
    private final int totalOrders;
    private final LocalDateTime joinedAt;

    private AdminMemberItemView(Long id,
                               String memberCode,
                               String name,
                               String email,
                               String phone,
                               MemberRole role,
                               MemberStatus status,
                               int totalOrders,
                               LocalDateTime joinedAt) {
        this.id = id;
        this.memberCode = memberCode;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.totalOrders = totalOrders;
        this.joinedAt = joinedAt;
    }

    public static AdminMemberItemView of(
            Long id,
            String memberCode,
            String name,
            String email,
            String phone,
            MemberRole role,
            MemberStatus status,
            int totalOrders,
            LocalDateTime joinedAt
    ) {
        return new AdminMemberItemView(
                id,
                memberCode,
                name,
                email,
                phone,
                role,
                status,
                totalOrders,
                joinedAt
        );
    }

    public static AdminMemberItemView from(Member member) {
        return AdminMemberItemView.of(
                member.getId(),
                generateMemberCode(member.getId()),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getRole(),
                member.getStatus(),
                0,  // TODO : 주문 도메인 연결 후 실제 주문 수 반영
                member.getCreatedAt()
        );
    }

    public String getJoinedAtText() {
        return joinedAt.format(JOIN_DATE_FORMAT);
    }


    private static String generateMemberCode(Long id) {
        if(id == null) {
            return "M000";
        }
        return String.format("M%03d", id);
    }
}
