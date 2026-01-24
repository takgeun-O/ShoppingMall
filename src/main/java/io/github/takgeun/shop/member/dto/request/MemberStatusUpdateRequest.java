package io.github.takgeun.shop.member.dto.request;

import io.github.takgeun.shop.member.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberStatusUpdateRequest {

    @NotNull(message = "status는 필수입니다.")
    private MemberStatus status;

    // 테스트용
    public static MemberStatusUpdateRequest of(MemberStatus status) {
        MemberStatusUpdateRequest request = new MemberStatusUpdateRequest();
        request.status = status;
        return request;
    }
}
