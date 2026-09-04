package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
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
@Transactional
public class MemberApiSecurityIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "password123!";

    @Autowired
    private MemberService memberService;

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
