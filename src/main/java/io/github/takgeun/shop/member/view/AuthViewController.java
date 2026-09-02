package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.LoginValidationSequence;
import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.view.form.LoginForm;
import io.github.takgeun.shop.member.view.form.SignupForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ViewController
@RequiredArgsConstructor
public class AuthViewController {

    /**
     * 기존 세션 인증, 인가 방식의 의존성
     */
//    private final AuthService authService;
//    private final MemberService memberService;

    /**
     * Spring Security 전환하면서 의존성 변경
     *
     * 참고) Spring Security 로그인 시 작동 방식
     * Controller
     * -> AuthenticationManager.authenticate() : 적절한 인증 담당자 선택
     * -> DaoAuthenticationProvider : 이메일과 비밀번호를 실제로 검증하는 Spring Security의 인증 담당자
     * -    ShopUserDetailsService로 회원 조회 -> ShopUserPrincipal 반환
     * -    PasswordEncoder로 비밀번호 비교
     * -> 인증된 Authentication 반환
     * -> SecurityContext 저장
     */
    private final AuthenticationManager authenticationManager;      // 아이디와 비밀번호가 맞는지 인증 (Spring Security의 진입점)
    private final SecurityContextRepository securityContextRepository;  // 인증 성공 결과를 다음 요청에서도 유지하도록 저장
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;  // 로그인 성공 후 세션 고정 보호, 동시 세션 제어, SessionRegistry 등록 등 세션 인증 처리를 적용하기 위해 호출
    private final MemberService memberService;

    // 회원가입 폼
    @GetMapping("/signup")
    public String signupForm(@ModelAttribute("form") SignupForm form) {
        return "auth/signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Validated(SignupValidationSequence.class) @ModelAttribute("form") SignupForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes ra) {

        // 비밀번호 확인 일치 검증 (confirmPassword 필드에 에러로 붙이기)
        // confirmPassword 가 비어 있을 때는 mismatch 검사 패스.
        boolean canCompare =
                !bindingResult.hasFieldErrors("password")
                        && !bindingResult.hasFieldErrors("confirmPassword")
                        && form.getPassword() != null && !form.getPassword().isBlank()
                        && form.getConfirmPassword() != null && !form.getConfirmPassword().isBlank();

        if (canCompare && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "비밀번호가 일치하지 않습니다.");
        }

        // 회원가입 실패 시 포워드
        if (bindingResult.hasErrors()) {
            return "auth/signup";   // 같은 요청 안에서 뷰 렌더링 -> Model 유지 -> 입력값 남아있음.
        }

        try {
            memberService.signup(form.getEmail(), form.getPassword(), form.getName(), form.getPhone());
        } catch (ConflictException e) {
            bindingResult.rejectValue("email", "duplicate", "이미 사용 중인 이메일입니다.");
            return "auth/signup";
        }

        ra.addFlashAttribute("success", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/login";
    }

    // 로그인 폼
    @GetMapping("/login")
    public String loginForm(@ModelAttribute("form") LoginForm form,
                            @RequestParam(required = false) String next,
                            @RequestParam(required = false) String reason,
                            Model model) {

        model.addAttribute("next", next);   // 로그인 후 직전 페이지로 이동할 next 저장

        if (reason != null) {
            switch (reason) {
                case "LOGIN_REQUIRED" -> model.addAttribute("infoMessage", "로그인이 필요한 서비스입니다.");
                case "ADMIN_REQUIRED" -> model.addAttribute("infoMessage", "관리자만 접근할 수 있습니다.");
                case "INACTIVE_ACCOUNT" -> model.addAttribute("errorMessage", "비활성 상태이거나 탈퇴한 계정이라 로그아웃되었습니다.");
                case "SESSION_EXPIRED" -> model.addAttribute("infoMessage", "로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
            }
        }

        return "auth/login";
    }

    // 로그인 처리 (세션 생성)
    @PostMapping("/login")
    public String login(
            @Validated(LoginValidationSequence.class) @ModelAttribute("form") LoginForm form,
            BindingResult bindingResult,
            @RequestParam(required = false) String next,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes ra,
            Model model
    ) {

        String safeNext = sanitizeNext(next);

        if (bindingResult.hasErrors()) {
            model.addAttribute("next", safeNext);
            return "auth/login";
        }

        try {
            /**
             * 기존 세션 기반의 인증
             */
//            Long memberId = authService.login(form.getEmail(), form.getPassword());
//            Member member = memberService.findById(memberId);

            // role 세션 저장
//            HttpSession session = request.getSession(true); // 세션이 있으면 그 세션을 반환하고 없으면 새로 만들어서 반환
//
//            session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);
//            session.setAttribute(SessionConst.LOGIN_ROLE, member.getRole());
//            session.setAttribute(SessionConst.LOGIN_MEMBER_NAME, member.getName());
//
//            ra.addFlashAttribute("success", "로그인되었습니다.");
//
//            return (safeNext != null) ? "redirect:" + safeNext : "redirect:/";

            /**
             * AuthenticationManager 인증으로 교체
             * 여기서 Authentication은 인증 완료 객체임
             * authenticationManager의 역할은 인증 결과를 반환하는 역할만 한다.
             *
             * 1. 이메일과 비밀번호 인증
             * AuthenticationManager는 인증 결과를 반환할 뿐,
             * SecurityContext 저장이나 세션 등록은 하지 않는다.
             *
             * 여기서 만든 authentication 객체의 상태는 아래와 같다.
             * principal   = 입력한 이메일
             * credentials = 입력한 평문 비밀번호
             * authenticated = false
             * authorities = 비어 있음
             *
             * 즉, 인증 결과가 아니라 "이 정보로 인증해 주세요."라는 요청서
             */
            Authentication authentication =
                    authenticationManager.authenticate(         // 참고: 실제 authenticationManager는 ProviderManager임.
                            UsernamePasswordAuthenticationToken // 이 토큰이 DaoAuthenticationProvider 호출하게 함.
                                    .unauthenticated(
                                            form.getEmail(),    // principal = 이메일
                                            form.getPassword()  // credentials(자격) = 사용자가 입력한 평문 비밀번호
                                    )
                            // 아직 검증 전이므로 unauthenticated() 상태
                    );

            /**
             * 2. 로그인 성공 후 세션 인증 전략 실행
             * CompositeSessionAuthenticationStrategy를 등록했다면
             * 다음 작업이 설정된 순서대로 실행된다.
             * - 동시 로그인 세션 수 검사
             * - 세션 고정 보호를 위한 세션 ID 변경
             * - 변경된 세션을 SessionRegistry에 등록
             */
            sessionAuthenticationStrategy.onAuthentication(
                    authentication,
                    request,
                    response
            );

            /**
             * 3. 인증 회원 정보 가져오기
             */
            ShopUserPrincipal principal =
                    (ShopUserPrincipal) authentication.getPrincipal();


            /**
             * 4. 인증 결과를 담을 SecurityContext 생성
             */
            SecurityContext securityContext =
                    SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(authentication);

            /**
             * 5. 현재 요청을 처리하는 Thread에서 인증 정보를 사용할 수 있도록 설정
             */
            SecurityContextHolder.setContext(securityContext);

            /**
             * 6. 세션 ID 변경이 끝난 최종 세션에 SecurityContext 저장
             */
            securityContextRepository.saveContext(
                    securityContext,
                    request,
                    response
            );

            /**
             * 7. 마지막 로그인 시각 갱신
             */
            memberService.recordSuccessfulLogin(principal.getMemberId());

            /**
             * 8. 기존 Interceptor와의 임시 호환
             * 기존 Interceptor용 세션 속성 저장
             *
             * SessionAuthenticationStrategy가 필요에 따라
             * 세션을 생성하거나 ID를 변경한 이후이므로,
             * 여기서 조회한 세션은 최종 세션이다.
             */
            HttpSession session = request.getSession(true); // 세션이 있으면 그 세션을 반환하고 없으면 새로 만들어서 반환
            session.setAttribute(
                    SessionConst.LOGIN_MEMBER_ID,
                    principal.getMemberId()
            );
            session.setAttribute(
                    SessionConst.LOGIN_ROLE,
                    principal.getRole()
            );
            session.setAttribute(
                    SessionConst.LOGIN_MEMBER_NAME,
                    principal.getName()
            );

            ra.addFlashAttribute(
                    "success",
                    "로그인되었습니다."
            );

            return safeNext != null
                    ? "redirect:" + safeNext
                    : "redirect:/";

        } catch (DisabledException e) {
            // ShopUserPrincipal의 isEnabled() 메서드에 의해 발생하는 예외
            // 회원상태가 ACTIVE면 isEnabled() true 반환 -> 다음 인증 단계 진행
            // 회원상태가 INACTIVE거나 WITHDRAWN이면 isEnabled()는 false 반환 -> 인증 Provider가 알아서 DisabledException 발생시킴
            bindingResult.reject(
                    "login.forbidden",
                    "비활성화된 계정입니다. 관리자에게 문의하세요."
            );
            model.addAttribute("next", safeNext);
            return "auth/login";
        } catch (SessionAuthenticationException e) {
            bindingResult.reject(
                    "login.session",
                    "현재 계정의 로그인 세션을 생성할 수 없습니다."
            );
            model.addAttribute("next", safeNext);
            return "auth/login";
        } catch (AuthenticationException e) {
            bindingResult.reject(
                    "login.unauthorized",
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );

            model.addAttribute("next", safeNext);
            return "auth/login";
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes ra) {

        HttpSession session = request.getSession(false);    // 세션 없으면 null 반환
        if (session != null) {
            session.invalidate();
        }
        ra.addFlashAttribute("success", "로그아웃되었습니다.");
        return "redirect:/";
    }

    // 로그인 후 next가 POST전용 URL일 경우 안전한 페이지로 바꾸기 위해
    private String sanitizeNext(String next) {
        if (next == null) {
            return null;
        }
        String n = next.trim();
        if (n.isEmpty()) {
            return null;
        }

        // 반드시 앱 내부 상대경로만 허용
        if (!n.startsWith("/") || n.startsWith("//")) {
            // redirect://evil.example.com 같은 외부 redirect도 방지
            return null;
        }

        // 민감/POST 전용/상태변경 URL 차단
        if (n.equals("/logout") ||
                n.equals("/orders") || n.startsWith("/orders?") ||
                n.equals("/cart/clear")) {
            return null;
        }
        return n;
    }
}
