package io.github.takgeun.shop.member.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MemberWithdrawalRequest(
        // 길이 제한을 걸지 않는다.
        // 과거 정책으로 생성된 비밀번호일 수도 있기 때문
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword
) {
}
