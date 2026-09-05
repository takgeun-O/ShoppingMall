package io.github.takgeun.shop.member.api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(

        @Size(
                min = 1,
                max = 50,
                message = "이름은 1자 이상 50자 이하입니다."
        )
        @Pattern(
                regexp = ".*\\S.*",
                message = "이름은 공백일 수 없습니다."
        )
        String name,

        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phone
) {
}
