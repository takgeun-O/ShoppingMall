package io.github.takgeun.shop.member.application.command;

import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;

@Getter
public class AdminMemberUpdateCommand {

    private final String name;
    private final String phone;
    private final MemberStatus status;

    private AdminMemberUpdateCommand(String name, String phone, MemberStatus status) {
        this.name = name;
        this.phone = phone;
        this.status = status;
    }

    public static AdminMemberUpdateCommand of(
            String name,
            String phone,
            MemberStatus status
    ) {
        return new AdminMemberUpdateCommand(
                name,
                phone,
                status
        );
    }
}
