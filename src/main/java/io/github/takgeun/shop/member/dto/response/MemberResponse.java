package io.github.takgeun.shop.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberResponse {
    private final Long id;
    private final String email;
    private final String name;
    private final String phone;
    private final String createdAt;
}
