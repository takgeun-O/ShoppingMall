package io.github.takgeun.shop.member.api.dto.request;

import io.github.takgeun.shop.member.domain.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.github.takgeun.shop.member.domain.PasswordPolicy.*;

/**
 * {
 *   "currentPassword": "현재비밀번호",
 *   "newPassword": "새로운비밀번호"
 * }
 */
public record PasswordChangeRequest(

        /**
         * DTO 검증 : 잘못된 HTTP 요청을 빠르게 400으로 거부
         *
         * 그리고 currentPassword에는 길이 제한을 적용하지 않는 것이 좋음.
         * (현재 비밀번호가 과거 정책으로 생성됐을 수도 있기 떄문)
         * 현재 비밀번호는 형식보다 실제 저장된 해시와 일치하는지가 중요함.
         */
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(
                min = MIN_LENGTH,
                max = MAX_LENGTH,
                message = "새 비밀번호는 8자 이상 20자 이하입니다."
        )
        String newPassword
) {
}
