package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.view.form.MemberEditForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberViewController {

    private final MemberService memberService;

    /**
     * 마이 페이지 조회
     * GET /members/me
     */
    @GetMapping("/me")
    public String me(HttpServletRequest request, Model model) {

        Long memberId = getLoginMemberId(request);
        if(memberId == null) {
            return redirectToLoginWithNext(request);    // 로그인 실패 시 로그인폼 이동
        }

        Member member = memberService.get(memberId);
        model.addAttribute("member", member);
        return "public/members/me";
    }

    /**
     * 마이페이지 수정폼
     */
    @GetMapping("/me/edit")
    public String editForm(HttpServletRequest request, Model model) {

        // 로그인 유효 확인
        Long memberId = getLoginMemberId(request);
        if(memberId == null) {
            return redirectToLoginWithNext(request);
        }

        Member member = memberService.get(memberId);

        MemberEditForm form = new MemberEditForm();
        form.setName(member.getName());
        form.setPhone(member.getPhone());

        model.addAttribute("form", form);
        return "public/members/edit";
    }

    /**
     * 마이페이지 수정 처리
     */
    @PostMapping("/me/edit")
    public String edit(@Valid @ModelAttribute("form") MemberEditForm form,
                       BindingResult bindingResult,
                       HttpServletRequest request,
                       RedirectAttributes ra) {
        Long memberId = getLoginMemberId(request);
        if(memberId == null) {
            return redirectToLoginWithNext(request);
        }

        if(bindingResult.hasErrors()) {
            return "public/members/edit";
        }

        // 비밀번호 변경은 UI상에서 분리할 예정 (여기서는 name, phone만 변경)
        memberService.updateProfile(memberId, form.getName(), null, form.getPhone());

        ra.addFlashAttribute("success", "회원 정보가 수정되었습니다.");
        return "redirect:/members/me";
    }

    /**
     * 탈퇴(비활성화) - 폼에서 POST로 호출
     */
    @PostMapping("/me/deactivate")
    public String deactivate(HttpServletRequest request, RedirectAttributes ra) {
        Long memberId = getLoginMemberId(request);
        if(memberId == null) {
            return redirectToLoginWithNext(request);
        }

        memberService.deactivate(memberId);

        // 세션 종료시키기
        HttpSession session = request.getSession(false);
        if(session != null) {
            session.invalidate();
        }

        ra.addFlashAttribute("success", "탈퇴 처리되었습니다.");
        return "redirect:/";
    }


    // 헬퍼 메소드

    private Long getLoginMemberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if(session == null) return null;

        Object idObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        return (idObj instanceof Long id) ? id : null;
    }

    private String redirectToLoginWithNext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String next = (query == null) ? uri : (uri + "?" + query);

        String url = UriComponentsBuilder.fromPath("/login")
                .queryParam("next", next)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        return "redirect:" + url;
    }
}
