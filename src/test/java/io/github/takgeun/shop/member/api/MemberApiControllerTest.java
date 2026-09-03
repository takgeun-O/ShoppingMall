package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이 테스트가 확인하는 흐름
 * Security 필터 없이 Controller의 Principal 처리,
 * Service 호출, DTO JSON 변환 검증
 *
 * SecurityContextHolder
 * → Authentication
 * → ShopUserPrincipal
 * → @AuthenticationPrincipal
 * → principal.getMemberId()
 * → MemberService.findById(memberId)
 * → MemberMeResponse
 * → JSON
 */
public class MemberApiControllerTest {

    private MemberService memberService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        /**
         * 가짜 MemberService 생성
         * → 실제 MemberApiController 생성
         * → Controller만 사용하는 MockMvc 구성
         * → API 예외 처리기 연결
         * → @AuthenticationPrincipal 처리기 연결
         * → MockMvc 완성
         */
        memberService = mock(MemberService.class);      // 테스트 환경에서는 Spring ApplicationContext를 사용하지 않아 Bean으로 끌어올 수 없으니 직접 만듦

        MemberApiController controller = new MemberApiController(memberService);    // 테스트 환경에서는 Spring ApplicationContext를 사용하지 않아 Bean으로 끌어올 수 없으니 직접 만듦

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiGlobalExceptionHandler())

        /**
         * standaloneSetup은 Spring Security MVC 설정을
         * 자동 등록하지 않으므로 직접 추가한다.
         */
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        /**
         * SecurityContextHolder의 저장 방식
         * 테스트 실행 스레드
         * └── ThreadLocal
         *     └── SecurityContext
         *         └── Authentication
         *             └── ShopUserPrincipal
         *
         * ThreadLocal은 스레드마다 독립적인 값을 보관하는 저장 공간
         *
         * 이전 테스트에서 수동으로 지정한 마지막 인증 정보가 같은 스레드를 사용하는 다음 테스트에
         * 남는 것을 방지한다.
         */
        SecurityContextHolder.clearContext();
    }

    @Test
    void 로그인한_회원의_정보를_조회한다() throws Exception {

        // given
        Long memberId = 1L;

        ShopUserPrincipal principal =
                new ShopUserPrincipal(
                        memberId,
                        "member@test.com",
                        "encoded-password",
                        "테스트회원",
                        MemberRole.USER,
                        MemberStatus.ACTIVE
                );
        // 여기까지는 ShopUserPrincipal을 만들었따고 해서 Spring Security가 자동으로 사용자를
        // 로그인 상태로 인식하지 않는다.
        // Spring Security가 현재 로그인 사용자를 인식하려면 이 사용자 정보를 Authentication 객체에 담아야 한다.

        /**
         * Spring Security에게 "현재 요청을 보내는 사용자는 로그인된 사용자다"라는 인증 상태를 알려주기 위해
         * authentication을 구한다.
         * Spring Security에서 Authentication은 당므 정보를 담는 표준 인터페이스임.
         * 누가 로그인했는가?
         * 어떤 자격 증명을 사용했는가?
         * 어떤 권한을 가지고 있는가?
         * 인증이 완료됐는가?
         */
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,       // 일반적으로 로그인할 때 사용한 비밀번호 같은 인증 수단. 인증이 끝난 후에는 보관 안하는 게 보안상 좋으니 null 사용
                principal.getAuthorities()
        );

        // 여기까지 해서 Authentication을 생성했다.
        // 하지만 Spring Security가 이 생성한 Authentication을 찾으려면
        // Authentication을 SecurityContext에 저장해야 한다.
        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        Member member = mock(Member.class);

        when(member.getId()).thenReturn(memberId);
        when(member.getEmail())
                .thenReturn("member@test.com");
        when(member.getName())
                .thenReturn("테스트회원");
        when(member.getPhone())
                .thenReturn("010-1234-5678");
        when(member.getRole())
                .thenReturn(MemberRole.USER);
        when(member.getStatus())
                .thenReturn(MemberStatus.ACTIVE);

        when(memberService.findById(memberId))
                .thenReturn(member);

        // when & then
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(memberId))
                .andExpect(jsonPath("$.email")
                        .value("member@test.com"))
                .andExpect(jsonPath("$.name")
                        .value("테스트회원"))
                .andExpect(jsonPath("$.phone")
                        .value("010-1234-5678"))
                .andExpect(jsonPath("$.role")
                        .value("USER"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))

                // 민감 정보가 응답에 포함되지 않아야 한다.
                .andExpect(jsonPath("$.password")
                        .doesNotExist());

        verify(memberService).findById(memberId);       // 테스트 실행 중 memberService.findById가 실제로 호출되었나?
    }

}
