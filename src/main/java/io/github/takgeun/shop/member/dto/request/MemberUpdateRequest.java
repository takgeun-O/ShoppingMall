package io.github.takgeun.shop.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberUpdateRequest {
    private final String name;
    private final String phone;
    // 주소는 나중에 추가
    // 이메일 변경 및 비밀번호 변경은 별도 DTO로 분리 ex) MemberPasswordChangeRequest 이런 식으로
}
