package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import io.github.takgeun.shop.global.security.SecurityContextService;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 이 테스트가 확인하는 흐름
 * Security 필터 없이 Controller의 Principal 처리,
 * Service 호출, DTO JSON 변환 검증
 * <p>
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
    private SecurityContextService securityContextService;
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
        securityContextService = mock(SecurityContextService.class);

        MemberApiController controller = new MemberApiController(memberService, securityContextService);    // 테스트 환경에서는 Spring ApplicationContext를 사용하지 않아 Bean으로 끌어올 수 없으니 직접 만듦

        /**
         * standaloneSetup() 에 Validator가 등록되어 있지 않아
         * @Valid 검증이 제대로 작동하지 않을 수 있음. -> Validator 등록하기
         */
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ApiGlobalExceptionHandler()
                )

                /**
                 * standaloneSetup은 Spring Security MVC 설정을
                 * 자동 등록하지 않으므로 직접 추가한다.
                 */
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setValidator(validator)        // 이렇게 해야 PasswordChangeRequest에 선언된 @NotBlank 등 검증이 실행됨
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

    @Test
    void 로그인한_회원의_이름과_전화번호를_수정한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        // Mockito를 사용해 Member 클래스의 가짜 객체(Mock 객체)를 만드는 코드
        Member updatedMember = mock(Member.class);

        when(updatedMember.getId())
                .thenReturn(memberId);
        when(updatedMember.getEmail())
                .thenReturn("member@test.com");
        when(updatedMember.getName())
                .thenReturn("변경된회원");
        when(updatedMember.getPhone())
                .thenReturn("010-9876-5432");
        when(updatedMember.getRole())
                .thenReturn(MemberRole.USER);
        when(updatedMember.getStatus())
                .thenReturn(MemberStatus.ACTIVE);

        /**
         * updateProfile() 실행 후 Controller가 수정된 회원 정보를
         * 다시 조회할 때 반환할 객체를 지정한다.
         */
        when(memberService.findById(memberId))
                .thenReturn(updatedMember);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "변경된회원",
                                            "phone": "010-9876-5432"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(memberId))
                .andExpect(jsonPath("$.email")
                        .value("member@test.com"))
                .andExpect(jsonPath("$.name")
                        .value("변경된회원"))
                .andExpect(jsonPath("$.phone")
                        .value("010-9876-5432"))
                .andExpect(jsonPath("$.role")
                        .value("USER"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.password")
                        .doesNotExist());

        /**
         * 로그인한 회원의 ID과 요청 JSON의 값이
         * Service에 정확하게 전달됐는지 확인한다.
         *
         * verity : Mockito Mock 객체의 메서드가 테스트 실행 중 실제로 호출됐는지 검사
         *
         * memberService.updateProfile()이 정확히 한 번 호출되었고,
         * 전달된 인자가 아래 값과 일치하는지 확인한다.
         *
         * memberId
         * "변경된회원"
         * "010-9876-5432"
         */
        verify(memberService).updateProfile(
                memberId,
                "변경된회원",
                "010-9876-5432"
        );

        /**
         * 수정 완료 후 최신 회원 정보를 다시 조회했는지 확인한다.
         */
        verify(memberService).findById(memberId);

        /**
         * 변경된 회원 정보로 현재 로그인 세션의 ShopUserPrincipal을 갱신했는지 확인한다.
         *
         * MockMvc가 실제로 생성한 request와 response 객체이므로
         * 정확한 객체를 직접 지정하지 않고 any()로 검사
         */
        verify(securityContextService).refreshPrincipal(
                eq(updatedMember),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)
        );
    }

    private void setAuthentication(
            Long memberId,
            String email,
            String name
    ) {

        ShopUserPrincipal principal = new ShopUserPrincipal(
                memberId,
                email,
                "encoded-password",
                name,
                MemberRole.USER,
                MemberStatus.ACTIVE
        );

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @Test
    void 이름만_전달하면_전화번호는_null로_Service에_전달한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        Member updatedMember = mock(Member.class);

        when(updatedMember.getId())
                .thenReturn(memberId);
        when(updatedMember.getEmail())
                .thenReturn("member@test.com");
        when(updatedMember.getName())
                .thenReturn("변경된회원");
        when(updatedMember.getPhone())
                .thenReturn("010-1234-5678");
        when(updatedMember.getRole())
                .thenReturn(MemberRole.USER);
        when(updatedMember.getStatus())
                .thenReturn(MemberStatus.ACTIVE);

        when(memberService.findById(memberId))
                .thenReturn(updatedMember);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "변경된회원"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("변경된회원"))
                .andExpect(jsonPath("$.phone")
                        .value("010-1234-5678"));

        verify(memberService).updateProfile(
                memberId,
                "변경된회원",
                null
        );
    }

    @Test
    void 전화번호만_전달하면_이름은_null로_Service에_전달한다()
            throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        Member updatedMember = mock(Member.class);

        when(updatedMember.getId())
                .thenReturn(memberId);
        when(updatedMember.getEmail())
                .thenReturn("member@test.com");
        when(updatedMember.getName())
                .thenReturn("기존회원");
        when(updatedMember.getPhone())
                .thenReturn("010-9876-5432");
        when(updatedMember.getRole())
                .thenReturn(MemberRole.USER);
        when(updatedMember.getStatus())
                .thenReturn(MemberStatus.ACTIVE);

        when(memberService.findById(memberId))
                .thenReturn(updatedMember);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "phone": "010-9876-5432"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("기존회원"))
                .andExpect(jsonPath("$.phone")
                        .value("010-9876-5432"));

        verify(memberService).updateProfile(
                memberId,
                null,
                "010-9876-5432"
        );
    }

    @Test
    void 이름이_공백이면_400을_반환한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "   "
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].reason")
                        .value(hasItem("이름은 공백일 수 없습니다.")));

        /*
         * Bean Validation에서 요청이 거절됐으므로
         * Controller 메서드와 Service는 실행되지 않아야 한다.
         */
        verify(memberService, never()).updateProfile(
                any(),
                any(),
                any()
        );

        verify(memberService, never()).findById(any());
    }

    @Test
    void 전화번호_형식이_잘못되면_400을_반환한다()
            throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "phone": "01012345678"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray());

        verify(memberService, never()).updateProfile(
                any(),
                any(),
                any()
        );

        verify(memberService, never()).findById(any());
    }

    /**
     * MemberApiController에 consumes가 있기 때문에 다음 테스트 진행
     */
    @Test
    void JSON이_아닌_회원정보_수정_요청은_415를_반환한다()
            throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        /**
         * text/plain 요청 본문은 MemberUpdateRequest로 변환할 수 없다.
         *
         * HandlerMethod가 선택된 이후 HttpMessageConverter 탐색 과정에서
         * HttpMediaTypeNotSupportedException이 발생하고,
         * ApiGlobalExceptionHandler가 JSON 415 응답으로 변환한다.
         */
        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(TEXT_PLAIN)
                                .content("name=변경된회원")
                )
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(
                        APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status")
                        .value(415))
                .andExpect(jsonPath("$.code")
                        .value("UNSUPPORTED_MEDIA_TYPE"));

        verify(memberService, never()).updateProfile(
                any(),
                any(),
                any()
        );
    }

    @Test
    void 회원정보_수정_JSON_문법이_잘못되면_400을_반환한다()
            throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "기존회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)

                                // 마지막 쉼표 문법 잘못
                                .content("""
                                        {
                                          "name": "변경된회원",
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"));

        verify(memberService, never()).updateProfile(
                any(),
                any(),
                any()
        );
    }

    @Test
    void 로그인한_회원은_비밀번호를_변경할_수_있다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "current-password",
                                            "newPassword": "changed-password"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(memberService).changePassword(
                memberId,
                "current-password",
                "changed-password"
        );

        // 비밀번호 변경 후에는 모든 세션을 만료시키니
        // securityContextService.refreshPrincipal()이 호출되면 안된다.
        verifyNoInteractions(securityContextService);
    }

    @Test
    void 현재_비밀번호가_공백이면_400을_반환한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "   ",
                                            "newPassword": "changed-password"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me/password"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())

                // 응답 JSON의 fieldErrors 배열 안에 field 값이 "currentPassword"인 항목이 하나 이상 있는지 검증
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("currentPassword")));

        /**
         * memberService가 Mock 객체인지 확인하고
         * 이후 지정한 메서드가 0번 호출됐는지 검사한다.
         *
         * Bean Validation 단계에서 거절되므로 컨트롤러 본문이 실행되지 않을 것이며
         * 결국 memberService도 호출되지 않음.
         */
        verify(memberService, never()).changePassword(
                any(),  // 어떤 값이든 상관없다.
                any(),
                any()
        );

        verifyNoInteractions(securityContextService);
    }

    @Test
    void 새_비밀번호가_8자보다_짧으면_400을_반환한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "current-password",
                                            "newPassword": "short"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("newPassword")));

        verify(memberService, never()).changePassword(
                any(),
                any(),
                any()
        );

        verifyNoInteractions(securityContextService);
    }

    @Test
    void 현재_비밀번호가_일치하지_않으면_오류응답을_반환한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        doThrow(new BusinessException(
                ErrorCode.INVALID_CURRENT_PASSWORD
        ))
                .when(memberService)
                .changePassword(
                        memberId,
                        "wrong-password",
                        "changed-password"
                );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "wrong-password",
                                            "newPassword": "changed-password"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CURRENT_PASSWORD"))
                .andExpect(jsonPath("$.message")
                        .value("현재 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me/password"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());

        verify(memberService).changePassword(
                memberId,
                "wrong-password",
                "changed-password"
        );
    }

    @Test
    void 새_비밀번호가_현재_비밀번호와_같으면_오류응답을_반환한다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        doThrow(new BusinessException(
                ErrorCode.PASSWORD_REUSE_NOT_ALLOWED
        ))
                .when(memberService)
                .changePassword(
                        memberId,
                        "current-password",
                        "current-password"
                );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "current-password",
                                            "newPassword": "current-password"
                                        }
                                        """)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("PASSWORD_REUSE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message")
                        .value("새 비밀번호는 현재 비밀번호와 달라야 합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me/password"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());

        verify(memberService).changePassword(
                memberId,
                "current-password",
                "current-password"
        );
    }

    @Test
    void 비밀번호_변경_JSON_문법이_잘못되면_400을_반환한다() throws Exception {

        // given
        setAuthentication(
                1L,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "current-password",
                                            "newPassword":
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me/password"));

        verify(memberService, never()).changePassword(
                any(),
                any(),
                any()
        );
    }

    @Test
    void 로그인한_회원은_현재_비밀번호를_확인하고_탈퇴할_수_있다() throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)

                                /**
                                 * 이 테스트에서 현재 비밀번호가 실제로 password123!인지는 알지 못함.
                                 * MemberService를 mock으로 만들었기 때문에 실제 DB나 BCrypt 비교가 실행되지 않음.
                                 *
                                 * Mock객체의 withdraw()는 별도 설정 없으면 아무 동작 없이 정상 종료함.
                                 *
                                 * 이 테스트에서 확인하고자 하는 것은 다음과 같음.
                                 * JSON의 currentPassword
                                 * → Controller가 읽음
                                 * → memberService.withdraw(memberId, currentPassword)에 전달
                                 * → Controller가 204 반환
                                 */
                                .content("""
                                        {
                                            "currentPassword": "password123!"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        /*
         * @AuthenticationPrincipal에서 얻은 회원 ID와
         * 요청 JSON의 현재 비밀번호가 Service로 전달됐는지 검증
         */
        verify(memberService).withdraw(
                memberId,
                "password123!"
        );

        /*
         * 회원 탈퇴 후에는 세션이 만료되므로
         * principal 갱신은 실행하지 않는다.
         */
        verifyNoInteractions(securityContextService);
    }

    @Test
    void 회원탈퇴_현재_비밀번호가_null이면_400을_반환한다() throws Exception {

        // given
        setAuthentication(
                1L,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                delete("/api/v1/members/me")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": null
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("currentPassword")));

        verify(memberService, never()).withdraw(
                any(),
                any()
        );

        verifyNoInteractions(securityContextService);
    }

    @Test
    void 회원탈퇴_현재_비밀번호가_공백이면_400을_반환한다() throws Exception {
        /**
         * 참고)
         * @NotBlank는 다음 값을 모두 거절함.
         * - null
         * - ""
         * - " "
         */

        // given
        setAuthentication(
                1L,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "currentPassword": "   "
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("currentPassword")))
                .andExpect(jsonPath("$.fieldErrors[*].reason")
                        .value(hasItem(
                                "현재 비밀번호는 필수입니다."
                        )));

        verify(memberService, never()).withdraw(
                any(),
                any()
        );

        verifyNoInteractions(securityContextService);
    }

    @Test
    void 회원탈퇴_현재_비밀번호가_누락되면_400을_반환한다() throws Exception {

        // given
        setAuthentication(
                1L,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                /**
                                 * 필드가 누락되면 Jackson이 currentPassword를 null로 역직렬화하고,
                                 * 그 다음 @NotBlank 검증에서 거절된다.
                                 */
                                .content("""
                                    {
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("currentPassword")));

        verify(memberService, never()).withdraw(
                any(),
                any()
        );
    }

    @Test
    void 회원탈퇴_JSON_문법이_잘못되면_400을_반환한다()
            throws Exception {

        // given
        setAuthentication(
                1L,
                "member@test.com",
                "회원"
        );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)

                                /**
                                 * 이 경우 DTO 검증까지 도달하지 못한다
                                 * JSON 파싱 실패
                                 * → HttpMessageNotReadableException
                                 * → ApiGlobalExceptionHandler
                                 * → MALFORMED_JSON
                                 */
                                .content("""
                                    {
                                      "currentPassword":
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());

        verify(memberService, never()).withdraw(
                any(),
                any()
        );

        verifyNoInteractions(securityContextService);
    }

    @Test
    void JSON이_아닌_회원탈퇴_요청은_415를_반환한다()
            throws Exception {

        // given
        setAuthentication(
                1L,
                "member@test.com",
                "회원"
        );

        /**
         * DELETE /api/v1/members/me
         * Content-Type: text/plain
         *         ↓
         * MockMvc가 요청 전달
         *         ↓
         * 회원탈퇴 Controller 메서드 검색
         *         ↓
         * 경로와 HTTP 메서드는 일치
         *         ↓
         * consumes = application/json 검사
         *         ↓
         * text/plain은 application/json과 불일치
         *         ↓
         * HttpMediaTypeNotSupportedException 발생
         *         ↓
         * ApiGlobalExceptionHandler가 예외 처리
         *         ↓
         * 415 UNSUPPORTED_MEDIA_TYPE JSON 반환
         */
        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .contentType(TEXT_PLAIN)
                                .content(
                                        "currentPassword=password123!"
                                )
                )
                .andExpect(status().isUnsupportedMediaType());

        verify(memberService, never()).withdraw(
                any(),
                any()
        );

        verifyNoInteractions(securityContextService);
    }

    @Test
    void 회원탈퇴_현재_비밀번호가_일치하지_않으면_400을_반환한다()
            throws Exception {

        // given
        Long memberId = 1L;

        setAuthentication(
                memberId,
                "member@test.com",
                "회원"
        );

        doThrow(new BusinessException(
                ErrorCode.INVALID_CURRENT_PASSWORD
        ))
                .when(memberService)
                .withdraw(
                        memberId,
                        "wrong-password"
                );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "currentPassword": "wrong-password"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CURRENT_PASSWORD"))
                .andExpect(jsonPath("$.message")
                        .value("현재 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());

        verify(memberService).withdraw(
                memberId,
                "wrong-password"
        );
    }
}
