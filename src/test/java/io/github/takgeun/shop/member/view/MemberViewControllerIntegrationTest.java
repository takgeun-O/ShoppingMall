package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MemberViewControllerIntegrationTest
        extends IntegrationTestSupport {

    @Autowired
    private MemberService memberService;

    private static final String PASSWORD =
            "pw12341234!";

    /**
     * 비로그인 요청은 MemberViewController까지 도달하지 않고
     * Spring Security가 로그인 페이지로 이동시킨다.
     */
    @Test
    void 비로그인_사용자가_마이페이지에_접근하면_로그인_페이지로_이동한다()
            throws Exception {

        mockMvc.perform(get("/members/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/members/me&reason=LOGIN_REQUIRED"
                ));
    }

    /**
     * 로그인 회원 ID를
     * ShopUserPrincipal에서 가져오는지 전체 흐름으로 검증한다.
     */
    @Test
    void 로그인_사용자는_마이페이지를_조회할_수_있다()
            throws Exception {

        // given
        String email =
                uniqueEmail("mypage");

        Long memberId =
                memberService.signup(
                        email,
                        PASSWORD,
                        "마이페이지회원",
                        "010-1111-2222"
                );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when & then
        mockMvc.perform(get("/members/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(view().name(
                        "public/members/me"
                ))
                .andExpect(model().attributeExists(
                        "member",
                        "orderSummary",
                        "recentOrders",
                        "wishlistCount"
                ));

        SecurityContext context =
                getSecurityContext(session);

        ShopUserPrincipal principal =
                (ShopUserPrincipal)
                        context.getAuthentication()
                                .getPrincipal();

        assertThat(principal.getMemberId())
                .isEqualTo(memberId);
    }

    @Test
    void 로그인_사용자는_회원정보_수정폼을_조회할_수_있다()
            throws Exception {

        // given
        String email =
                uniqueEmail("edit-form");

        memberService.signup(
                email,
                PASSWORD,
                "수정전회원",
                "010-2222-3333"
        );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when & then
        mockMvc.perform(get("/members/me/edit")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "public/members/edit"
                ))
                .andExpect(model().attributeExists(
                        "form",
                        "member"
                ))
                .andExpect(model().attribute(
                        "form",
                        org.hamcrest.Matchers.hasProperty(
                                "name",
                                org.hamcrest.Matchers.is(
                                        "수정전회원"
                                )
                        )
                ))
                .andExpect(model().attribute(
                        "form",
                        org.hamcrest.Matchers.hasProperty(
                                "phone",
                                org.hamcrest.Matchers.is(
                                        "010-2222-3333"
                                )
                        )
                ));
    }

    /**
     * 회원 정보 수정 시 다음 세 곳이 모두 갱신되는지 확인한다.
     *
     * 1. DB의 Member
     * 2. 현재 SecurityContext
     * 3. ShopUserPrincipal의 회원 이름
     */
    @Test
    void 회원정보를_수정하면_DB와_SecurityContext의_Principal이_갱신된다()
            throws Exception {

        // given
        String email =
                uniqueEmail("update");

        Long memberId =
                memberService.signup(
                        email,
                        PASSWORD,
                        "수정전회원",
                        "010-3333-4444"
                );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when
        mockMvc.perform(post("/members/me/edit")
                        .session(session)
                        .param(
                                "name",
                                "수정후회원"
                        )
                        .param(
                                "phone",
                                "010-5555-6666"
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/members/me/edit"
                ))
                .andExpect(flash().attribute(
                        "success",
                        "회원 정보가 수정되었습니다."
                ));

        // then: DB 회원정보
        Member updatedMember =
                memberService.findById(memberId);

        assertThat(updatedMember.getName())
                .isEqualTo("수정후회원");

        assertThat(updatedMember.getPhone())
                .isEqualTo("010-5555-6666");

        // then: 세션에 저장된 SecurityContext
        SecurityContext securityContext =
                getSecurityContext(session);

        Authentication authentication =
                securityContext.getAuthentication();

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.isAuthenticated())
                .isTrue();

        assertThat(authentication.getPrincipal())
                .isInstanceOf(
                        ShopUserPrincipal.class
                );

        ShopUserPrincipal refreshedPrincipal =
                (ShopUserPrincipal)
                        authentication.getPrincipal();

        assertThat(refreshedPrincipal.getMemberId())
                .isEqualTo(memberId);

        assertThat(refreshedPrincipal.getName())
                .isEqualTo("수정후회원");

        assertThat(refreshedPrincipal.getUsername())
                .isEqualTo(email);
    }

    /**
     * 검증 실패 시 서비스 수정 로직을 실행하지 않고
     * 수정 화면을 다시 출력한다.
     */
    @Test
    void 회원정보_수정값이_올바르지_않으면_수정폼을_다시_보여준다()
            throws Exception {

        // given
        String email =
                uniqueEmail("validation");

        Long memberId =
                memberService.signup(
                        email,
                        PASSWORD,
                        "검증회원",
                        "010-4444-5555"
                );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when & then
        mockMvc.perform(post("/members/me/edit")
                        .session(session)
                        .param("name", "")
                        .param(
                                "phone",
                                "잘못된-전화번호"
                        ))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "public/members/edit"
                ))
                .andExpect(model().attributeHasErrors(
                        "form"
                ))
                .andExpect(model().attributeExists(
                        "member"
                ));

        Member member =
                memberService.findById(memberId);

        assertThat(member.getName())
                .isEqualTo("검증회원");

        assertThat(member.getPhone())
                .isEqualTo("010-4444-5555");
    }

    /**
     * 회원 탈퇴 후 현재 세션과 SecurityContext가 제거되어
     * 같은 세션으로 마이페이지에 다시 접근할 수 없어야 한다.
     */
    @Test
    void 회원을_탈퇴하면_현재_세션이_종료되고_마이페이지에_재접근할_수_없다()
            throws Exception {

        // given
        String email =
                uniqueEmail("deactivate");

        Long memberId =
                memberService.signup(
                        email,
                        PASSWORD,
                        "탈퇴회원",
                        "010-6666-7777"
                );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        // when
        mockMvc.perform(post("/members/me/deactivate")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // then: 회원 상태
        Member deactivatedMember =
                memberService.findById(memberId);

        assertThat(deactivatedMember.isActive())
                .isFalse();

        // 현재 세션은 SecurityContextLogoutHandler가 무효화
        assertThat(session.isInvalid())
                .isTrue();

        /*
         * 무효화된 MockHttpSession을 다시 전달하면
         * IllegalStateException이 발생할 수 있으므로,
         * 별도 비로그인 요청으로 접근 차단을 확인한다.
         */
        mockMvc.perform(get("/members/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/members/me&reason=LOGIN_REQUIRED"
                ));
    }

    private MockHttpSession loginAndGetSession(
            String email,
            String password
    ) throws Exception {

        MvcResult result =
                mockMvc.perform(post("/login")
                                .param("email", email)
                                .param(
                                        "password",
                                        password
                                ))
                        .andExpect(status()
                                .is3xxRedirection())
                        .andReturn();

        MockHttpSession session =
                (MockHttpSession)
                        result.getRequest()
                                .getSession(false);

        assertThat(session)
                .isNotNull();

        return session;
    }

    private SecurityContext getSecurityContext(
            MockHttpSession session
    ) {
        Object value =
                session.getAttribute(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY
                );

        assertThat(value)
                .isInstanceOf(
                        SecurityContext.class
                );

        return (SecurityContext) value;
    }

    private String uniqueEmail(
            String prefix
    ) {
        return prefix
                + "-"
                + UUID.randomUUID()
                + "@test.com";
    }
}