package io.github.takgeun.shop.member.dto.request;

import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;

@Getter
public class AdminMemberStatusUpdateRequest {

    private final MemberStatus status;

    private AdminMemberStatusUpdateRequest(MemberStatus status) {
        this.status = status;
    }

    public static AdminMemberStatusUpdateRequest of(MemberStatus status) {
        return new AdminMemberStatusUpdateRequest(status);
    }
}
