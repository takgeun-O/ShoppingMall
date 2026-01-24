package io.github.takgeun.shop.member.dto.request;

import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberRoleUpdateRequest {

    @NotNull(message = "role은 필수입니다.")
    private MemberRole role;

    // 테스트용
    public static MemberRoleUpdateRequest of(MemberRole role) {
        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest();
        request.role = role;
        return request;
    }
}
