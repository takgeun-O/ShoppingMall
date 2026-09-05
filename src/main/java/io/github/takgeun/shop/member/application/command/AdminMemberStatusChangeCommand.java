package io.github.takgeun.shop.member.application.command;

import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;

@Getter
public class AdminMemberStatusChangeCommand {

    private final MemberStatus status;

    private AdminMemberStatusChangeCommand(MemberStatus status) {
        this.status = status;
    }

    public static AdminMemberStatusChangeCommand of(MemberStatus status) {
        return new AdminMemberStatusChangeCommand(status);
    }
}
