package io.github.takgeun.shop.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginResponse {
    private final Long memberId;
    private final String name;
}
