package io.github.takgeun.shop.global.security.session;

import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/**
 * 세션 등록이 확인되면 회원 ID 기준으로 모든 세션을 만료시키는 클래스
 */
@Service
@RequiredArgsConstructor
public class MemberSessionService {

    private final SessionRegistry sessionRegistry;

    public void expireAllByMemberId(Long memberId) {
        if (memberId == null) {
            return;
        }

        sessionRegistry.getAllPrincipals()
                .stream()
                .filter(ShopUserPrincipal.class::isInstance)
                .map(ShopUserPrincipal.class::cast)
                .filter(principal ->
                        memberId.equals(principal.getMemberId()))
                .flatMap(principal ->
                        sessionRegistry
                                .getAllSessions(
                                        principal,
                                        false
                                )
                                .stream()
                )
                .forEach(SessionInformation::expireNow);
        /**
         * expireNow()
         * → SessionInformation을 만료 상태로 표시
         * → 해당 세션으로 다음 요청
         * → ConcurrentSessionFilter가 만료 감지
         * → 로그아웃·세션 종료 처리
         */
    }
}
