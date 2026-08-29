package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@Rollback
public class AuthenticationBaselineIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "pw12341234!";

    /**
     * 현재 UserAuthInterceptor 기준선
     *
     * 로그인하지 않은 사용자가 보호된 사용자 페이지에 접근하면
     * 로그인 페이지로 이동시키고 원래 요청 경로를 next에 저장한다.
     */
    @Test
    void 비로그인_사용자가_주문_페이지에_접근하면_로그인_페이지로_리다이렉트된다() throws Exception {

        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/orders&reason=LOGIN_REQUIRED"
                ));
    }

    /**
     * 현재 AdminAuthInterceptor 기준선
     */
    @Test
    void 비로그인_사용자가_관리자_페이지에_접근하면_로그인_페이지로_리다이렉트된다() throws Exception {

        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/admin&reason=LOGIN_REQUIRED"
                ));
    }

    /**
     * 로그인은 헀지만 관리자 권한이 없는 경우
     */
    @Test
    void 일반_회원이_관리자_페이지에_접근하면_403을_반환한다() throws Exception {

        Long memberId = createMember("user");
        Member member = memberService.findById(memberId);

        MockHttpSession session = authenticatedSession(member);

        mockMvc.perform(get("/admin")
                .session(session))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(model().attribute("status", 403))
                .andExpect(model().attribute(
                        "message",
                        "관리자 권한이 필요합니다."
                ))
                .andExpect(model().attribute("path", "/admin"));
    }

    /**
     * 세션 권한과 DB 회원 상태가 모두 정상인 관리자는 접근할 수 있다.
     */
    @Test
    void 활성_관리자는_관리자_페이지에_접근할_수_있다()
            throws Exception {

        Long memberId = createMember("admin");
        memberService.changeRole(memberId, MemberRole.ADMIN);

        Member admin = memberService.findById(memberId);
        MockHttpSession session = authenticatedSession(admin);

        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("dashboard"));
    }

    /**
     * 로그인 성공 시 기존 세션 인증 정보가 어떻게 저장되는지 고정한다.
     *
     * Spring Security 전환 후에는 이 세션 속성들이
     * SecurityContext 기반 인증으로 대체될 수 있다.
     */
    @Test
    void 로그인에_성공하면_회원정보를_세션에_저장한다()
            throws Exception {

        String email = uniqueEmail("login");
        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "로그인회원",
                "010-1111-2222"
        );

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "success",
                        "로그인되었습니다."
                ))
                .andExpect(request().sessionAttribute(
                        SessionConst.LOGIN_MEMBER_ID,
                        memberId
                ))
                .andExpect(request().sessionAttribute(
                        SessionConst.LOGIN_ROLE,
                        MemberRole.USER
                ))
                .andExpect(request().sessionAttribute(
                        SessionConst.LOGIN_MEMBER_NAME,
                        "로그인회원"
                ));
    }

    /**
     * 인증 필터/인터셉터에서 전달한 next 경로 복귀 기준선
     */
    @Test
    void 로그인에_성공하면_next_경로로_리다이렉트된다()
            throws Exception {

        String email = uniqueEmail("next");

        memberService.signup(
                email,
                PASSWORD,
                "복귀경로회원",
                "010-2222-3333"
        );

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", PASSWORD)
                        .param(
                                "next",
                                "/products/1?categoryId=10&sort=latest"
                        ))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/products/1?categoryId=10&sort=latest"
                ));
    }

    /**
     * 외부 URL로 리다이렉트되는 Open Redirect 방지 기준선
     */
    @Test
    void 외부_URL이_next로_전달되면_기본_페이지로_이동한다()
            throws Exception {

        String email = uniqueEmail("unsafe-next");

        memberService.signup(
                email,
                PASSWORD,
                "외부경로회원",
                "010-3333-4444"
        );

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", PASSWORD)
                        .param("next", "https://evil.example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    /**
     * 로그인 실패는 예외 화면으로 전환하지 않고
     * 로그인 폼에 검증 오류를 표시한다.
     */
    @Test
    void 비밀번호가_일치하지_않으면_로그인_폼을_다시_보여준다()
            throws Exception {

        String email = uniqueEmail("wrong-password");

        memberService.signup(
                email,
                PASSWORD,
                "비밀번호실패회원",
                "010-4444-5555"
        );

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeHasErrors("form"));
    }

    /**
     * 비활성 회원은 비밀번호가 맞더라도 로그인할 수 없다.
     */
    @Test
    void 비활성_회원은_로그인할_수_없다()
            throws Exception {

        String email = uniqueEmail("inactive");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "비활성회원",
                "010-5555-6666"
        );

        memberService.deactivate(memberId);

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(request().sessionAttributeDoesNotExist(
                        SessionConst.LOGIN_MEMBER_ID
                ));
    }

    /**
     * 현재 로그아웃은 HttpSession 전체를 무효화한다.
     */
    @Test
    void 로그아웃하면_기존_세션이_무효화된다()
            throws Exception {

        Long memberId = createMember("logout");
        Member member = memberService.findById(memberId);

        MockHttpSession session = authenticatedSession(member);

        mockMvc.perform(post("/logout")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "success",
                        "로그아웃되었습니다."
                ));

        assertThat(session.isInvalid()).isTrue();
    }

    /**
     * AdminAuthInterceptor가 DB 상태를 다시 확인하는지 검증한다.
     *
     * 세션에는 관리자 정보가 남아 있어도 DB에서 비활성화되었다면
     * 세션을 폐기하고 로그인 페이지로 이동해야 한다.
     */
    @Test
    void 로그인_후_비활성화된_관리자가_접근하면_세션을_무효화한다()
            throws Exception {

        Long memberId = createMember("inactive-admin");
        memberService.changeRole(memberId, MemberRole.ADMIN);

        Member admin = memberService.findById(memberId);
        MockHttpSession session = authenticatedSession(admin);

        memberService.deactivate(memberId);

        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/admin&reason=INACTIVE_ACCOUNT"
                ));

        assertThat(session.isInvalid()).isTrue();
    }

    private Long createMember(String prefix) {
        return memberService.signup(
                uniqueEmail(prefix),
                PASSWORD,
                prefix + "회원",
                "010-1234-5678"
        );
    }

    private String uniqueEmail(String prefix) {
        return prefix + System.nanoTime() + "@test.com";
    }

    private MockHttpSession authenticatedSession(Member member) {
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
                SessionConst.LOGIN_MEMBER_ID,
                member.getId()
        );
        session.setAttribute(
                SessionConst.LOGIN_ROLE,
                member.getRole()
        );
        session.setAttribute(
                SessionConst.LOGIN_MEMBER_NAME,
                member.getName()
        );

        return session;
    }
}
