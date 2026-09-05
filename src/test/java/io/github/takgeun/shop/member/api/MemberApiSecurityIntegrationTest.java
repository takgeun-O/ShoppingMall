package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 회원가입
 * → POST /login
 * → AuthenticationManager
 * → DaoAuthenticationProvider
 * → ShopUserDetailsService
 * → 로그인 세션 생성
 * → GET /api/v1/members/me
 * → SecurityFilterChain
 * → MemberApiController
 * → MemberService
 * → MyBatis
 */

// 테스트에 @Transactional이 있으니 매 테스트마다 트랜잭션 롤백된다는 것 생각하기 -> AFTER_COMMIT 리스너 실행 안될 수 있음
// 비트랜잭션 테스트 따로 만들어서 커밋 후 세션 만료 확인 테스트 진행할 것. (MemberSessionExpirationIntegrationTest 파일 ㄱㄱ)
@Transactional
public class MemberApiSecurityIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "password123!";

    @Autowired
    private MemberService memberService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 비로그인_사용자가_내_정보_API를_요청하면_401을_반환한다() throws Exception {

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    @Test
    void 로그인한_사용자가_자신의_회원정보를_조회한다() throws Exception {

        // given
        String email = "member-api-" + System.nanoTime() + "@test.com";
        String name = "회원API테스트";
        String phone = "010-1234-5678";

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                name,
                phone
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(get("/api/v1/members/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id")
                        .value(memberId))
                .andExpect(jsonPath("$.email")
                        .value(email))
                .andExpect(jsonPath("$.name")
                        .value(name))
                .andExpect(jsonPath("$.phone")
                        .value(phone))
                .andExpect(jsonPath("$.role")
                        .value("USER"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))

                // 비밀번호가 JSON으로 노출되지 않는지 확인
                .andExpect(jsonPath("$.password")
                        .doesNotExist());
    }

    @Test
    void CSRF_토큰_없이_회원정보를_수정하면_403을_반환한다()
            throws Exception {

        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "변경된회원"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void 비로그인_사용자가_회원정보를_수정하면_401을_반환한다()
            throws Exception {

        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "변경된회원"
                                        }
                                        """)
                )
                .andDo(print())
                /**
                 * PATCH /api/v1/members/me
                 * → CsrfFilter
                 * → CSRF 토큰 없음
                 * → 403 Forbidden
                 * → 인증 여부 검사까지 도달하지 않음
                 */
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    @Test
    void 로그인한_회원이_이름과_전화번호를_수정한다()
            throws Exception {

        // given
        String email = uniqueEmail("member-patch");
        String originalName = "기존회원";
        String originalPhone = "010-1111-1111";
        String changedName = "변경된회원";
        String changedPhone = "010-9876-5432";

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                originalName,
                originalPhone
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "phone": "%s"
                                        }
                                        """.formatted(
                                        changedName,
                                        changedPhone
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id")
                        .value(memberId))
                .andExpect(jsonPath("$.email")
                        .value(email))
                .andExpect(jsonPath("$.name")
                        .value(changedName))
                .andExpect(jsonPath("$.phone")
                        .value(changedPhone))
                .andExpect(jsonPath("$.password")
                        .doesNotExist());

        // DB에 실제 변경됐는지 확인
        Member updatedMember =
                memberService.findById(memberId);

        assertThat(updatedMember.getName())
                .isEqualTo(changedName);
        assertThat(updatedMember.getPhone())
                .isEqualTo(changedPhone);
    }

    @Test
    void 회원정보를_수정한_세션으로_다시_조회하면_변경값이_반환된다()
            throws Exception {

        // given
        String email = uniqueEmail("member-patch-get");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "기존회원",
                "010-1111-1111"
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "변경된회원",
                                          "phone": "010-9876-5432"
                                        }
                                        """)
                )
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(
                        get("/api/v1/members/me")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(memberId))
                .andExpect(jsonPath("$.name")
                        .value("변경된회원"))
                .andExpect(jsonPath("$.phone")
                        .value("010-9876-5432"));

        /**
         * SecurityContextService.refreshPrincipal() 이 제대로 principal을 변경했는지 증명해야함.
         * 단순히 get "/api/v1/members/me" 요청을 하면 DB에서 가져오기 때문에 아래 검증을 추가로 한다.
         *
         * 프로필 변경
         * → DB 회원정보 변경
         * → 새로운 ShopUserPrincipal 생성
         * → SecurityContext 교체
         * → 세션에 변경된 SecurityContext 저장
         */
        SecurityContext securityContext =
                (SecurityContext) session.getAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
                );

        assertThat(securityContext).isNotNull();

        ShopUserPrincipal refreshPrincipal =
                (ShopUserPrincipal) securityContext
                        .getAuthentication()
                        .getPrincipal();

        assertThat(refreshPrincipal.getMemberId())
                .isEqualTo(memberId);

        assertThat(refreshPrincipal.getName())
                .isEqualTo("변경된회원");
    }

    @Test
    void 이름만_부분_수정하면_전화번호는_유지된다()
            throws Exception {

        // given
        String email = uniqueEmail("member-patch-name");
        String originalPhone = "010-1111-1111";

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "기존회원",
                originalPhone
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .with(csrf())
                                .session(session)
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
                        .value(originalPhone));

        Member updatedMember =
                memberService.findById(memberId);

        assertThat(updatedMember.getName())
                .isEqualTo("변경된회원");
        assertThat(updatedMember.getPhone())
                .isEqualTo(originalPhone);
    }

    @Test
    void 잘못된_회원정보_수정_요청은_400을_반환한다()
            throws Exception {

        // given
        String email = uniqueEmail("member-patch-invalid");

        memberService.signup(
                email,
                PASSWORD,
                "기존회원",
                "010-1111-1111"
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "   ",
                                          "phone": "잘못된 전화번호"
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
    }

    @Test
    void 비로그인_사용자가_비밀번호를_변경하면_401을_반환한다() throws Exception {

        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "password123!",
                                            "newPassword": "changed123!"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me/password"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    @Test
    void 로그인한_사용자가_CSRF_토큰_없이_비밀번호를_변경하면_403을_반환한다() throws Exception {

        // given
        String email = uniqueEmail("password-csrf");

        memberService.signup(
                email,
                PASSWORD,
                "회원",
                "010-1111-1111"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "password123!",
                                            "newPassword": "changed123!"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    /**
     * DB의 암호화된 비밀번호가 실제로 바뀌었는지 확인
     */
    @Test
    void 로그인한_회원은_비밀번호를_변경할_수_있다() throws Exception {

        // given
        String email = uniqueEmail("password-change");
        String currentPassword = PASSWORD;
        String newPassword = "changed123!";

        Long memberId = memberService.signup(
                email,
                currentPassword,
                "회원",
                "010-1111-1111"
        );

        MockHttpSession session = loginAndGetSession(email, currentPassword);

        String previousEncodedPassword =
                memberService.findById(memberId)
                        .getPassword();

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "%s",
                                            "newPassword": "%s"
                                        }
                                        """.formatted(
                                        currentPassword,
                                        newPassword
                                ))
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Member updatedMember = memberService.findById(memberId);

        assertThat(updatedMember.getPassword())
                .isNotEqualTo(previousEncodedPassword);

        assertThat(passwordEncoder.matches(
                newPassword,
                updatedMember.getPassword()
        )).isTrue();

        assertThat(passwordEncoder.matches(
                currentPassword,
                updatedMember.getPassword()
        )).isFalse();
    }

    @Test
    void 현재_비밀번호가_일치하지_않으면_비밀번호를_변경하지_않는다() throws Exception {

        // given
        String email = uniqueEmail("wrong-current-password");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "회원",
                "010-1111-1111"
        );

        MockHttpSession session = loginAndGetSession(
                email,
                PASSWORD
        );

        String originalEncodedPassword = memberService.findById(memberId).getPassword();

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "wrong-password",
                                            "newPassword": "changed123!"
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

        Member unchangedMember = memberService.findById(memberId);

        // 암호문 자체가 변경되지 않았나?
        assertThat(unchangedMember.getPassword())
                .isEqualTo(originalEncodedPassword);

        // 기존 평문 비밀번호가 여전히 유효한가?
        assertThat(passwordEncoder.matches(
                PASSWORD,
                unchangedMember.getPassword()
        )).isTrue();
    }

    @Test
    void 새_비밀번호가_현재_비밀번호와_같으면_400을_반환한다() throws Exception {

        // given
        String email = uniqueEmail("password-reuse");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "회원",
                "010-1111-1111"
        );

        MockHttpSession session = loginAndGetSession(
                email,
                PASSWORD
        );

        String originalEncodedPassword = memberService.findById(memberId).getPassword();

        // when & then
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "%s",
                                          "newPassword": "%s"
                                        }
                                        """.formatted(
                                        PASSWORD,
                                        PASSWORD
                                ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("PASSWORD_REUSE_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message")
                        .value("새 비밀번호는 현재 비밀번호와 달라야 합니다."))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());

        Member unchangedMember = memberService.findById(memberId);

        assertThat(unchangedMember.getPassword())
                .isEqualTo(originalEncodedPassword);
    }

    @Test
    void 새_비밀번호가_정책에_맞지_않으면_400을_반환한다() throws Exception {

        // given
        String email = uniqueEmail("invalid-password");

        memberService.signup(
                email,
                PASSWORD,
                "회원",
                "010-1111-1111"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        /**
         * PATCH 요청
         * → 세션에서 SecurityContext 복원
         * → 로그인된 회원으로 인증됨
         * → Controller 진입 준비
         * → PasswordChangeRequest 검증
         * → newPassword가 짧음
         * → 400 INVALID_INPUT
         */
        mockMvc.perform(
                        patch("/api/v1/members/me/password")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                            "currentPassword": "password123!",
                                            "newPassword": "short"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem(
                                "newPassword"
                        )));
    }

    @Test
    void 비로그인_사용자가_회원탈퇴를_요청하면_401을_반환한다() throws Exception {

        // when
        mockMvc.perform(
                delete("/api/v1/members/me")
                /**
                 * CSRF 토큰을 제공해야 CsrfFilter를 통과하고
                 * 인증 여부 검사까지 도달한다.
                 *
                 * 만약 빼면 CsrfFilter에서 차단되어 403 Forbidden이 나옴
                 */
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "password123!"
                                }
                                """)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                APPLICATION_JSON
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists())
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    @Test
    void 로그인한_회원이_CSRF_토큰_없이_탈퇴를_요청하면_403을_반환한다()
            throws Exception {

        // given
        String email = uniqueEmail(
                "withdraw-csrf"
        );

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "회원",
                "010-1111-2222"
        );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "currentPassword": "password123!"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());

        /*
         * CSRF 단계에서 요청이 차단됐으므로
         * 실제 회원 상태는 바뀌지 않아야 한다.
         */
        Member member =
                memberService.findById(memberId);

        assertThat(member.getStatus())
                .isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 로그인한_회원이_정상적으로_탈퇴하면_204를_반환하고_WITHDRAWN으로_변경된다()
            throws Exception {

        // given
        String email = uniqueEmail(
                "withdraw-success"
        );

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "탈퇴회원",
                "010-2222-3333"
        );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        Member beforeWithdrawMember =
                memberService.findById(memberId);

        assertThat(beforeWithdrawMember.getStatus())
                .isEqualTo(MemberStatus.ACTIVE);

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "currentPassword": "%s"
                                    }
                                    """.formatted(PASSWORD))
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        /*
         * MemberApiController
         * → MemberService.withdraw()
         * → Member.withdraw()
         * → Repository 저장
         */
        Member withdrawnMember =
                memberService.findById(memberId);

        assertThat(withdrawnMember.getStatus())
                .isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    void 현재_비밀번호가_일치하지_않으면_400을_반환하고_ACTIVE를_유지한다() throws Exception {

        // given
        String email = uniqueEmail(
                "withdraw-wrong-password"
        );

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "탈퇴실패회원",
                "010-3333-4444"
        );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when & then
        mockMvc.perform(
                        delete("/api/v1/members/me")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "currentPassword": "wrong-password"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                APPLICATION_JSON
                        ))
                .andExpect(jsonPath("$.timestamp")
                        .exists())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CURRENT_PASSWORD"))
                .andExpect(jsonPath("$.message")
                        .value("현재 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/members/me"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());

        /*
         * 비밀번호 확인에 실패했으므로
         * 회원 상태가 변경되지 않아야 한다.
         */
        Member unchangedMember =
                memberService.findById(memberId);

        assertThat(unchangedMember.getStatus())
                .isEqualTo(MemberStatus.ACTIVE);
    }

    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        Object session = result.getRequest().getSession(false);

        assertThat(session)
                .isInstanceOf(MockHttpSession.class);

        return (MockHttpSession) session;
    }

    private String uniqueEmail(String prefix) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@test.com";
    }
}
