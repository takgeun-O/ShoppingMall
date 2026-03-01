package io.github.takgeun.shop.member.api.dto.request;

import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberProfileUpdateRequest {

    @NotBlank(message = "이름을 입력해주세요.", groups = SignupValidationSequence.class)
    private String name;

    @NotBlank(message = "전화번호를 입력해주세요.", groups = SignupValidationSequence.class)
    @Pattern(
            regexp = "^010-\\d{4}-\\d{4}$",
            message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)",
            groups = SignupValidationSequence.class
    )
    private String phone;
}
