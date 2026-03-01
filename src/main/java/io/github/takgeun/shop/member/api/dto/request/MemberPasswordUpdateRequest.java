package io.github.takgeun.shop.member.api.dto.request;

import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberPasswordUpdateRequest {

    @NotBlank(message = "비밀번호를 입력해주세요.", groups = SignupValidationSequence.class)
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다.", groups = SignupValidationSequence.class)
    private String password;
}
