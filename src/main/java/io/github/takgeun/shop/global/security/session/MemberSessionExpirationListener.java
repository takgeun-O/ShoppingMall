package io.github.takgeun.shop.global.security.session;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 정보 변경
 * → 이벤트 발행
 * → DB 트랜잭션 커밋
 * → 기존 세션 만료
 *
 * 즉 트랜잭션이 롤백되면 Listener는 실행되지 않음
 */
@Component
@RequiredArgsConstructor
public class MemberSessionExpirationListener {

    private final MemberSessionService memberSessionService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(MemberSessionExpirationEvent event) {
        memberSessionService.expireAllByMemberId(event.memberId());
    }
}
