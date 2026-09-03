package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.member.application.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
