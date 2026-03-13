package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.LoginValidationSequence;
import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import io.github.takgeun.shop.member.application.AuthService;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.view.form.LoginForm;
import io.github.takgeun.shop.member.view.form.SignupForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final AuthService authService;
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

        if(canCompare && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "비밀번호가 일치하지 않습니다.");
        }

        // 회원가입 실패 시 포워드
        if(bindingResult.hasErrors()) {
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
    public String loginForm(@ModelAttribute("form")LoginForm form,
                            @RequestParam(required = false) String next,
                            @RequestParam(required = false) String reason,
                            Model model) {

        model.addAttribute("next", next);   // 로그인 후 직전 페이지로 이동할 next 저장

        if(reason != null) {
            String message = switch (reason) {
                case "LOGIN_REQUIRED" -> "로그인이 필요한 서비스입니다. 로그인 후 계속 진행해주세요.";
                case "ADMIN_REQUIRED" -> "관리자만 접근할 수 있는 페이지입니다.";
                default -> null;
            };
            model.addAttribute("infoMessage", message);
        }

        return "auth/login";
    }

    // 로그인 처리 (세션 생성)
    @PostMapping("/login")
    public String login(@Validated(LoginValidationSequence.class) @ModelAttribute("form") LoginForm form,
                        BindingResult bindingResult,
                        @RequestParam(required = false) String next,
                        HttpServletRequest request,
                        RedirectAttributes ra,
                        Model model) {

        String safeNext = sanitizeNext(next);

        if(bindingResult.hasErrors()) {
            model.addAttribute("next", next);
            return "auth/login";
        }

        try {
            Long memberId = authService.login(form.getEmail(), form.getPassword());

            // role 세션 저장 (AdminAuthInterceptor 쪽에서 사용)
            Member member = memberService.findById(memberId);
            HttpSession session = request.getSession(true); // 세션이 있으면 그 세션을 반환하고 없으면 새로 만들어서 반환
            session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);
            session.setAttribute(SessionConst.LOGIN_ROLE, member.getRole());
            session.setAttribute(SessionConst.LOGIN_MEMBER_NAME, member.getName());

            ra.addFlashAttribute("success", "로그인되었습니다.");

            return (safeNext != null) ? "redirect:" + safeNext : "redirect:/";
        } catch (UnauthorizedException e) {
            // 이메일/비밀번호 불일치
            bindingResult.reject("login.unauthorized", "이메일 또는 비밀번호가 올바르지 않습니다.");    // 폼 상단에 전체 에러로 띄우기
            model.addAttribute("next", next);
            return "auth/login";
        } catch (ForbiddenException e) {
            // 비활성 회원 로그인 시도
            bindingResult.reject("login.forbidden", "비활성화된 계정입니다. 관리자에게 문의하세요.");
            model.addAttribute("next", next);
            return "auth/login";
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes ra) {

        HttpSession session = request.getSession(false);    // 세션 없으면 null 반환
        if(session != null) {
            session.invalidate();
        }
        ra.addFlashAttribute("success", "로그아웃되었습니다.");
        return "redirect:/";
    }

    // 로그인 후 next가 POST전용 URL일 경우 안전한 페이지로 바꾸기 위해
    private String sanitizeNext(String next) {
        if(next == null) return null;
        String n = next.trim();
        if(n.isEmpty()) return null;

        // 반드시 앱 내부 상대경로만 허용
        if(!n.startsWith("/")) return null;

        // 민감/POST 전용/상태변경 URL 차단
        if( n.equals("/logout") ||
            n.equals("/orders") || n.startsWith("/orders?") ||
            n.equals("/cart/clear") || n.startsWith("/admin")) {
            return null;
        }
        return n;
    }
}
