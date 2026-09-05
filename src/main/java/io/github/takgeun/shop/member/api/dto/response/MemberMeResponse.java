package io.github.takgeun.shop.member.api.dto.response;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;

import java.time.LocalDateTime;

public record MemberMeResponse(
        Long id,
        String email,
        String name,
        String phone,
        MemberRole role,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {

    public static MemberMeResponse from(Member member) {
        return new MemberMeResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getRole(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getLastLoginAt()
        );
    }
}
