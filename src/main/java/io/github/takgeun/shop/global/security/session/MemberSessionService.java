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

        /**
         * 특정 memberId를 가진 회원이 여러 기기나 브라우저에서
         * 로그인해 만든 모든 세션을 찾아 만료 상태로 표시한다.
         *
         * 예를 들어 회원ID가 10L인 사용자가
         * - 맥북 Chrome 세션
         * - 아이폰 Safari 세션
         * - 회사 PC Chrome 세션
         * 에 로그인했다고 가정할 때
         *
         * expireAllByMemberId(10L):
         * 호출 시 이 회원의 3개 세션을 모두 찾아 expireNow() 호출함.
         *
         * SessionRegistry에 등록된 모든 Principal 조회
         * → ShopUserPrincipal만 남김
         * → 안전하게 ShopUserPrincipal로 형변환(cast)
         * → memberId가 일치하는 Principal만 남김
         * → 해당 Principal들의 모든 활성 세션 조회
         * → 하나의 세션 흐름으로 합침
         * → 각 세션을 만료 상태로 표시
         */

        // SessionRegistry가 알고 있는 모든 로그인 사용자를 가져오기
        // 반환타입: List<Object> (Spring Security가 Principal 타입을 ShopUserPrincipal로 한정하지 않기 때문)
        sessionRegistry.getAllPrincipals()
                .stream()
                .filter(ShopUserPrincipal.class::isInstance)

                // 앞에서 실제 객체가 ShopUserPrincipal인지 검사했지만
                // Java의 Stream 타입은 자동으로 Stream<ShopUserPrincipal>로 바뀌지 않는다.
                // 따라서 cast()로 타입을 명시적으로 형변환한다.
                // 즉 object -> (ShopUserPrincipal) object == ShopUserPrincipal.class::cast 같은거임.
                // 단 주의할 점은 앞의 filter() 작업으로 꼭 걸러낸 후에 cast()해야 한다. (그냥 세트라고 생각!)
                // filter()로 못 걸러내고 cast()하면 다른 타입의 Principal이 있을 때 ClassCastException이 발생할 수 있음.
                .map(ShopUserPrincipal.class::cast)

                // 입력받은 회원ID와 Principal의 회원ID가 같은 객체만 남긴다.
                .filter(principal ->
                        memberId.equals(principal.getMemberId()))
                .flatMap(       // flatMap을 써서 여러 Stream을 평평하게 펼쳐서 바로 접근해서 처리하기 쉽게 변형
                        principal ->
                        sessionRegistry
                                // .getAllSessions(): 해당 principal에 등록된 세션 정보를 가져온다.
                                // false : 이미 만료된 세션은 제외하고 아직 활성 상태인 세션만 가져와라.
                                .getAllSessions(
                                        principal,
                                        false
                                )
                                .stream()
                )

                // 위 작업결과 타입은 SessionInformation 타입임
                // SessionInformation
                //├─ Principal
                //├─ Session ID
                //├─ 마지막 요청 시간
                //└─ 만료 여부
                // 찾아낸 모든 SessionInformation에 expireNow() 실행
                // 단 주의할 점은 실제 HttpSession을 그 자리에서 즉시 invalidate()하는 메서드가 아님
                // 이 세션은 더 이상 사용하면 안된다는 만료 표시를 하는 역할을 함. (즉 sessionInformation.isExpired()가 true인데 HttpSession 객체는 아직 존재하고 있을 수 있다는 것)
                // 이 만료 표시를 확인하는 놈은 ConcurrentSessionFilter임.
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
