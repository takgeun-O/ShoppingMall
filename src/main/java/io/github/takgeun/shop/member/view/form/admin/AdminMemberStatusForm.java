package io.github.takgeun.shop.member.view.form.admin;

import io.github.takgeun.shop.member.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMemberStatusForm {

    @NotNull(message = "회원 상태는 필수입니다.")
    private MemberStatus status;
}
