package io.github.takgeun.shop.global.security.session;

public record MemberSessionExpirationEvent(
        Long memberId
) {
}
