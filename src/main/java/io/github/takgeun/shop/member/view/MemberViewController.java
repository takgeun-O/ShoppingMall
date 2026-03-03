package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.application.MyPageQueryService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.view.dto.MyPageMemberView;
import io.github.takgeun.shop.member.view.dto.MyPageOrderSummaryView;
import io.github.takgeun.shop.member.view.dto.MyPageRecentOrderView;
import io.github.takgeun.shop.member.view.form.MemberEditForm;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberViewController {

    private final MemberService memberService;
    private final MyPageQueryService myPageQueryService;

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

        String grade = "GOLD";
        int point = 15000;
        int couponCount = 5;

        MyPageMemberView memberView = MyPageMemberView.from(member, grade, point, couponCount);

        model.addAttribute("member", memberView);
        model.addAttribute("orderSummary", myPageQueryService.getOrderSummary(memberId));  // 주문 현황
        model.addAttribute("recentOrders", myPageQueryService.getRecentOrders(memberId, 5));   // 최근 주문
        model.addAttribute("wishlistCount", 8); // 찜한 상품

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

        String grade = "GOLD";
        int point = 15000;
        int couponCount = 5;
        MyPageMemberView memberView = MyPageMemberView.from(member, grade, point, couponCount);

        model.addAttribute("form", form);
        model.addAttribute("member", memberView);
        return "public/members/edit";
    }

    /**
     * 마이페이지 수정 처리
     * 수정 성공 후 redirect:/members/me/edit (수정페이지로 이동)
     * 수정 실패 시 public/members/edit 제자리로 포워드
     */
    @PostMapping("/me/edit")
    public String edit(@Validated(SignupValidationSequence.class) @ModelAttribute("form") MemberEditForm form,
                       BindingResult bindingResult,
                       HttpServletRequest request,
                       Model model,
                       RedirectAttributes ra) {

        Long memberId = getLoginMemberId(request);
        if(memberId == null) {
            return redirectToLoginWithNext(request);
        }

        if(bindingResult.hasErrors()) {
            // edit 페이지에서 member 카드를 쓰면 에러 시에도 다시 주입해줘야 화면 깨짐 방지
            Member member = memberService.get(memberId);
            String grade = "GOLD";
            int point = 15000;
            int couponCount = 5;
            model.addAttribute("member", MyPageMemberView.from(member, grade, point, couponCount));

            return "public/members/edit";
        }

        // 비밀번호 변경은 UI상에서 분리할 예정 (여기서는 name, phone만 변경)
        memberService.updateProfile(memberId, form.getName(), null, form.getPhone());

        // memberService.updateProfile() 에서 trim, 정규화, 길이제한 같은 것을 할 경우
        // 폼값이 아닌 저장 후 조회한 값으로 세션 갱신하는 게 안전함
        Member updated = memberService.get(memberId);

        // 세션에 저장된 헤더용 이름도 갱신하기. (마이페이지에서 변경 시 상단 헤더도 변경되도록 세션 갱신)
        HttpSession session = request.getSession(false);
        if(session != null) {
            session.setAttribute(SessionConst.LOGIN_MEMBER_NAME, updated.getName());
        }

        ra.addFlashAttribute("success", "회원 정보가 수정되었습니다.");
        return "redirect:/members/me/edit";
    }

    /**
     * 탈퇴(비활성화) - edit페이지에서 POST로 호출
     * 탈퇴 성공 후 redirect:/ (홈으로 이동)
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
