package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.global.security.SecurityContextService;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.global.security.session.MemberSessionService;
import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.application.MyPageQueryService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.view.dto.MyPageMemberView;
import io.github.takgeun.shop.member.view.form.MemberEditForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ViewController
@RequiredArgsConstructor
@RequestMapping("/members/me")
public class MemberViewController {

    private static final String MEMBER_PAGE_VIEW = "public/members/me";
    private static final String MEMBER_EDIT_VIEW = "public/members/edit";
    private static final String MEMBER_EDIT_REDIRECT = "redirect:/members/me/edit";

    private final MemberService memberService;
    private final MyPageQueryService myPageQueryService;
    private final SecurityContextService securityContextService;

    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    /**
     * 마이 페이지 조회
     * GET /members/me
     * 로그인 여부는 SecurityConfig에서 검사함.
     */
    @GetMapping()
    public String me(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            Model model
    ) {

        Long memberId = principal.getMemberId();

        Member member = memberService.findById(memberId);

        String grade = "GOLD";
        int point = 15000;
        int couponCount = 5;

        MyPageMemberView memberView = MyPageMemberView.from(member, grade, point, couponCount);

        model.addAttribute("member", memberView);
        model.addAttribute("orderSummary", myPageQueryService.getOrderSummary(memberId));  // 주문 현황
        model.addAttribute("recentOrders", myPageQueryService.getRecentOrders(memberId, 5));   // 최근 주문
        model.addAttribute("wishlistCount", 8); // 찜한 상품

        return MEMBER_PAGE_VIEW;
    }

    /**
     * 마이페이지 수정폼
     */
    @GetMapping("/edit")
    public String editForm(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            Model model) {

        Member member = memberService.findById(principal.getMemberId());

        MemberEditForm form = new MemberEditForm();
        form.setName(member.getName());
        form.setPhone(member.getPhone());

        String grade = "GOLD";
        int point = 15000;
        int couponCount = 5;
        MyPageMemberView memberView = MyPageMemberView.from(member, grade, point, couponCount);

        model.addAttribute("form", form);
        model.addAttribute("member", memberView);
        return MEMBER_EDIT_VIEW;
    }

    /**
     * 마이페이지 수정 처리
     * 참고로 HTML의 <form></form>은 기본적으로 GET, POST 방식만 지원한다.
     *
     * 수정 성공 후 redirect:/members/me/edit (수정페이지로 이동)
     * 수정 실패 시 public/members/edit 제자리로 포워드
     */
    @PostMapping("/edit")
    public String edit(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            @Validated(SignupValidationSequence.class) @ModelAttribute("form") MemberEditForm form,
                       BindingResult bindingResult,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       Model model,
                       RedirectAttributes ra
    ) {

        Long memberId = principal.getMemberId();

        if(bindingResult.hasErrors()) {
            // edit 페이지에서 member 카드를 쓰면 에러 시에도 다시 주입해줘야 화면 깨짐 방지
            Member member = memberService.findById(memberId);
            String grade = "GOLD";
            int point = 15000;
            int couponCount = 5;

            model.addAttribute(
                    "member",
                    MyPageMemberView.from(member, grade, point, couponCount)
            );

            return MEMBER_EDIT_VIEW;
        }

        // 비밀번호 변경은 UI상에서 분리할 예정 (여기서는 name, phone만 변경)
        memberService.updateProfile(
                memberId,
                form.getName(),
                form.getPhone()
        );

        // memberService.updateProfile() 에서 trim, 정규화, 길이제한 같은 것을 할 경우
        // 폼값이 아닌 저장 후 조회한 값으로 세션 갱신하는 게 안전함
        Member updatedMember = memberService.findById(memberId);

        /**
         * 기존 세션 기반 헤더 이름 갱신을 대신한다.
         *
         * 다음 요청부터 GlobalViewModelAdvice가
         * 변경된 Principal의 이름을 사용할 수 있다.
         */
        securityContextService.refreshPrincipal(
                updatedMember,
                request,
                response
        );

        ra.addFlashAttribute(
                "success",
                "회원 정보가 수정되었습니다."
        );

        return MEMBER_EDIT_REDIRECT;
    }

    /**
     * 회원 탈퇴
     *
     * 회원 비활성화 트랜잭션이 커밋되면
     * MemberSessionExpirationEvent의 AFTER_COMMIT 리스너가
     * 해당 회원의 모든 로그인 세션을 만료 처리한다.
     *
     * 현재 요청에서 사용 중인 세션은 즉시 로그아웃 처리한다.
     */
    @PostMapping("/deactivate")
    public String deactivate(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes ra
    ) {

        Long memberId = principal.getMemberId();

        /**
         * 회원 상태 변경
         * -> 트랜잭션 커밋
         * -> MemberSessionExpirationEvent 처리
         * -> 해당 회원의 모든 SessionInformation 만료
         */
        memberService.deactivate(memberId);


        /**
         * expireNow()는 SessionInformation만 만료 상태로 표시한다.
         *
         * 그렇기 때문에 현재 요청의 SecurityContext와 HttpSession은
         * 바로 정리하기 위해 명시적으로 로그아웃 처리한다.
         */
        logoutHandler.logout(
                request,
                response,
                authentication
        );

        ra.addFlashAttribute("success", "탈퇴 처리되었습니다.");

        return "redirect:/";
    }
}
