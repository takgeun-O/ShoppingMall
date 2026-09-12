package io.github.takgeun.shop.category.api.admin;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminCategoryApiSecurityIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "pw12341234!";
    @Autowired
    private MemberService memberService;
    @Autowired
    private CategoryService categoryService;

    @Test
    void 비로그인_사용자가_카테고리를_생성하면_401을_반환한다() throws Exception {

        /**
         * MockMvc가 POST 요청 생성
         *         ↓
         * csrf()가 유효한 CSRF 토큰 추가
         *         ↓
         * Spring SecurityFilterChain 진입
         *         ↓
         * CsrfFilter의 CSRF 검증 통과
         *         ↓
         * AuthorizationFilter가 접근 권한 검사
         *         ↓
         * 현재 SecurityContext에 Authentication 없음
         *         ↓
         * 인증이 필요한 관리자 경로임을 확인
         *         ↓
         * AuthenticationException 처리 시작
         *         ↓
         *  // @RestControllerAdvice는 일반적으로 DispatcherServlet 내부의 Controller 처리 과정에서 발생한 예외를 담당
         *  // Security Filter에서 차단된 요청은 AuthenticationEntryPoint가 직접 응답한다.
         * API용 AuthenticationEntryPoint 실행
         *         ↓
         * 401 + AUTHENTICATION_REQUIRED JSON 반환
         *         ↓
         * Controller는 실행되지 않음
         */
        mockMvc.perform(
                post("/api/v1/admin/categories")
                        // 유효한 CSRF를 넣어서 일부러 통과시키기
                        // 만약 csrf()를 없애면 보안 필터가 먼저 요청을 차단할 수 있음. -> 테스트하려던 비로그인 사용자의 관리자 API 접근까지 도달 못함
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "노트북",
                                    "parentId": null
                                }
                                """)
        )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void 일반_회원이_카테고리를_생성하면_403을_반환한다()
            throws Exception {

        // given
        String email = uniqueEmail("category-user");

        memberService.signup(
                email,
                PASSWORD,
                "일반회원",
                "010-1111-2222"
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        /**
         * 일반 회원 가입
         *         ↓
         * 실제 로그인 후 세션 발급
         *         ↓
         * 세션과 CSRF 토큰을 담아 POST 요청
         *         ↓
         * SecurityContext에서 로그인 정보 복원
         *         ↓
         * CSRF 검사 통과
         *         ↓
         * /api/v1/admin/**의 ADMIN 권한 검사
         *         ↓
         * 현재 사용자는 USER 권한만 보유
         *         ↓
         * AccessDeniedException 발생
         *         ↓
         * API용 AccessDeniedHandler 실행
         *         ↓
         * 403 + ACCESS_DENIED JSON 응답
         */
        // when & then
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .with(csrf())
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "노트북",
                                          "parentId": null
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code")
                        .value("ACCESS_DENIED"));
    }

    @Test
    void 관리자라도_CSRF_토큰이_없으면_403을_반환한다()
            throws Exception {

        // given
        MockHttpSession adminSession =
                createAdminSession("csrf-admin");

        /**
         * 관리자 로그인 세션 생성
         *         ↓
         * 관리자 세션을 담아 POST 요청
         *         ↓
         * CSRF 토큰은 넣지 않음
         *         ↓
         * SecurityContext에서 관리자 인증정보 복원
         *         ↓
         * CsrfFilter가 토큰 검사
         *         ↓
         * CSRF 토큰이 없으므로 검사 실패
         *         ↓
         * AccessDeniedHandler가 403 응답 생성
         *         ↓
         * Controller는 실행되지 않음
         */
        // when & then
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "노트북",
                                          "parentId": null
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_최상위_카테고리를_생성할_수_있다()
            throws Exception {

        // given
        MockHttpSession adminSession =
                createAdminSession("create-admin");

        String categoryName =
                "전자-" + UUID.randomUUID();

        // when & then
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .with(csrf())
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "parentId": null
                                        }
                                        """.formatted(categoryName))
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name")
                        .value(categoryName))
                .andExpect(jsonPath("$.parentId")
                        .doesNotExist())
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"));
    }

    @Test
    void 관리자는_하위_카테고리를_생성할_수_있다()
            throws Exception {

        // given
        MockHttpSession adminSession =
                createAdminSession("child-admin");

        Long parentId = categoryService.create(
                "전자-" + UUID.randomUUID(),
                null
        );

        String childName =
                "노트북-" + UUID.randomUUID();

        // when & then
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .with(csrf())
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "parentId": %d
                                        }
                                        """.formatted(
                                        childName,
                                        parentId
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value(childName))
                .andExpect(jsonPath("$.parentId")
                        .value(parentId));
    }

    @Test
    void 관리자는_카테고리_상태를_변경할_수_있다()
            throws Exception {

        // given
        MockHttpSession adminSession =
                createAdminSession("status-admin");

        Long categoryId = categoryService.create(
                "상태변경-" + UUID.randomUUID(),
                null
        );

        // when
        mockMvc.perform(
                        patch(
                                "/api/v1/admin/categories/{categoryId}/status",
                                categoryId
                        )
                                .with(csrf())
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "INACTIVE"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(categoryId))
                .andExpect(jsonPath("$.status")
                        .value("INACTIVE"));

        // then: 응답만 아니라 실제 DB 결과까지 확인
        Category category =
                categoryService.getAdmin(categoryId);

        assertThat(category.getStatus())
                .isEqualTo(CategoryStatus.INACTIVE);
    }

    private MockHttpSession createAdminSession(String prefix) throws Exception {

        String email = uniqueEmail(prefix);

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "관리자",
                "010-1234-5678"
        );

        memberService.changeRole(memberId, MemberRole.ADMIN);

        return loginAndGetSession(
                email,
                PASSWORD
        );
    }

    private MockHttpSession loginAndGetSession(
            String email,
            String password
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", email)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result
                        .getRequest()
                        .getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    private String uniqueEmail(String prefix) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@test.com";
    }
}
