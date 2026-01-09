package io.github.takgeun.shop.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest {
    private final String email;
    private final String password;
}
