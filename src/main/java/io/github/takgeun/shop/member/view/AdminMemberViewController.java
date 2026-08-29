package io.github.takgeun.shop.member.view;

import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.CheckoutValidationSequence;
import io.github.takgeun.shop.member.application.AdminMemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.dto.request.AdminMemberStatusUpdateRequest;
import io.github.takgeun.shop.member.dto.request.AdminMemberUpdateRequest;
import io.github.takgeun.shop.member.view.dto.admin.AdminMemberDetailView;
import io.github.takgeun.shop.member.view.dto.admin.AdminMemberEditView;
import io.github.takgeun.shop.member.view.dto.admin.AdminMemberPageView;
import io.github.takgeun.shop.member.view.form.admin.AdminMemberEditForm;
import io.github.takgeun.shop.member.view.form.admin.AdminMemberSearchCondition;
import io.github.takgeun.shop.member.view.form.admin.AdminMemberStatusForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/members")
public class AdminMemberViewController {

    private final AdminMemberService adminMemberService;

    /**
     * 관리자 회원 관리 페이지
     * GET /admin/members
     */
    @GetMapping
    public String members(
            @RequestParam(required = false) String nameQuery,
            @RequestParam(required = false) String emailQuery,
            @RequestParam(required = false, defaultValue = "ALL") String statusFilter,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,      // 뷰에서 안 넘어오는데 사실상 10으로 고정해서 쓸 것
            Model model,
            HttpSession session
    ) {

        requireAdmin(session);

        log.info("관리자 회원 관리 페이지 진입");

        AdminMemberSearchCondition searchCondition = AdminMemberSearchCondition.of(
                nameQuery,
                emailQuery,
                statusFilter,
                page,
                pageSize
        );

        AdminMemberPageView pageView = adminMemberService.getAdminMemberPage(searchCondition);

        // model.addAttribute("totalMemberCount", totalMemberCount) ...
        // 위 방식처럼 pageView를 잘게 쪼개서 뷰로 내려보내면 컨트롤러도 지저분해지고 뷰 입장에서 어디서 내려온 데이터인지 구분이 쉽지 않음.
        // 나중에 summary에 항목이 추가되면 컨트롤러와 뷰를 같이 수정해야 할 일이 발생함. (뷰에서 계산하면 뷰만 수정하면 됨)
        // 아래처럼 통째로 pageView를 보내서 뷰에서 계산해주는 게 더 낫다.
        model.addAttribute("pageView", pageView);
        model.addAttribute("searchCondition", searchCondition);

        return "admin/members/list";
    }

    /**
     * 관리자 회원 상세보기 페이지
     * GET /admin/members/{memberId}
     */
    @GetMapping("/{memberId}")
    public String memberDetail(@PathVariable Long memberId,
                               Model model,
                               HttpSession session) {

        requireAdmin(session);

        log.info("관리자 회원 상세 페이지 진입, memberId={}", memberId);

        AdminMemberDetailView member = adminMemberService.getAdminMemberDetail(memberId);
        model.addAttribute("member", member);

        return "admin/members/detail";
    }

    /**
     * 관리자 회원 수정 페이지
     * GET /admin/members/{memberId}/edit
     */
    @GetMapping("/{memberId}/edit")
    public String editMemberForm(@PathVariable Long memberId,
                                 Model model,
                                 HttpSession session) {

        requireAdmin(session);

        log.info("관리자 회원 수정 페이지 진입, memberId={}", memberId);

        AdminMemberEditView member = adminMemberService.getAdminMemberEditView(memberId);   // 읽기 전용 표시용 (id, email, joinDate, lastLogin, totalOrders, totalSpent, lastOrderDate)
        AdminMemberEditForm form = AdminMemberEditForm.from(member);                        // 실제 수정용 (name, phone, status)

        model.addAttribute("member", member);
        model.addAttribute("form", form);

        return "admin/members/edit";
    }

    /**
     * 관리자 회원 수정 처리 (회원 수정 페이지에서 행하는 모든 수정 처리)
     * POST /admin/members/{memberId}/edit
     */
    @PostMapping("/{memberId}/edit")
    public String editMember(@PathVariable Long memberId,
                             @Validated(CheckoutValidationSequence.class) @ModelAttribute("form") AdminMemberEditForm form,
                             BindingResult bindingResult,
                             Model model,
                             HttpSession session,
                             RedirectAttributes ra) {

        requireAdmin(session);

        log.info("관리자 회원 수정 처리 memberId={}", memberId);

        if(bindingResult.hasErrors()) {
            AdminMemberEditView member = adminMemberService.getAdminMemberEditView(memberId);
            model.addAttribute("member", member);
            model.addAttribute("form", form);
            return "admin/members/edit";
        }

        // 서비스 호출용 DTO (이름, 폰번호, 상태만 변경)
        AdminMemberUpdateRequest request = AdminMemberUpdateRequest.of(
                form.getName(),
                form.getPhone(),
                form.getStatus()
        );

        adminMemberService.updateMember(memberId, request);

        ra.addFlashAttribute("success", "회원 정보가 수정되었습니다.");
        return "redirect:/admin/members/" + memberId;
    }

    /**
     * 관리자 회원 상태 변경 처리 (회원 상세정보 페이지에서 바꾸는 것)
     * POST /admin/members/{memberId}/status
     */
    @PostMapping("/{memberId}/status")
    public String changeMemberStatus(
            @PathVariable Long memberId,
            @Valid @ModelAttribute("form") AdminMemberStatusForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes ra) {

        requireAdmin(session);

        log.info("관리자 회원 상태 변경 처리, memberId={}, status={}", memberId, form.getStatus());

        if(bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "회원 상태 변경 요청이 올바르지 않습니다.");
            return "redirect:/admin/members/" + memberId;
        }

        AdminMemberStatusUpdateRequest request = AdminMemberStatusUpdateRequest.of(form.getStatus());
        adminMemberService.changeMemberStatus(memberId, request);

        ra.addFlashAttribute("success", "회원 상태가 변경되었습니다.");
        return "redirect:/admin/members/" + memberId;
    }

    /**
     * 관리자 회원 탈퇴 (회원 상세정보 페이지에서 탈퇴 처리)
     * POST /admin/members/{memberId}/withdraw
     */
    @PostMapping("/{memberId}/withdraw")
    public String withdrawMember(
            @PathVariable Long memberId,
            HttpSession session,
            RedirectAttributes ra) {

        requireAdmin(session);

        log.info("관리자 회원 탈퇴 처리(회원상세정보페이지), memberId={}", memberId);

        adminMemberService.withdrawMember(memberId);

        ra.addFlashAttribute("success", "회원이 탈퇴 처리되었습니다.");
        return "redirect:/admin/members/" + memberId;
    }

    private void requireAdmin(HttpSession session) {

        if(session == null) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if(!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
    }
}
