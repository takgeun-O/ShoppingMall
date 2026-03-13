package io.github.takgeun.shop.member.dto.request;

import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;

@Getter
public class AdminMemberUpdateRequest {

    private final String name;
    private final String phone;
    private final MemberStatus status;

    private AdminMemberUpdateRequest(String name, String phone, MemberStatus status) {
        this.name = name;
        this.phone = phone;
        this.status = status;
    }

    public static AdminMemberUpdateRequest of(
            String name,
            String phone,
            MemberStatus status
    ) {
        return new AdminMemberUpdateRequest(
                name,
                phone,
                status
        );
    }
}
