package io.github.takgeun.shop.category.view;

import io.github.takgeun.shop.category.application.AdminCategoryQueryService;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategoryEditView;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategoryItemView;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategoryPageView;
import io.github.takgeun.shop.category.view.form.CategoryCreateForm;
import io.github.takgeun.shop.category.view.form.CategoryEditForm;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
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

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryViewController {

    private final CategoryService categoryService;
    private final AdminCategoryQueryService adminCategoryQueryService;

    /**
     * 관리자 카테고리 관리 페이지
     * GET /admin/categories
     */
    @GetMapping
    public String list(Model model, HttpSession session) {

        requireAdmin(session);
        log.info("관리자 카테고리 관리 페이지 진입");

        AdminCategoryPageView pageView = adminCategoryQueryService.getCategoryPage();

        if(!model.containsAttribute("form")) {
            CategoryCreateForm form = new CategoryCreateForm();
            model.addAttribute("form", form);
        }

        model.addAttribute("categories", pageView.getCategories());
        model.addAttribute("summary", pageView.getSummary());
        return "admin/categories/list";
    }

    // 카테고리 생성 폼
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CategoryCreateForm());
        model.addAttribute("categories", categoryService.getTopCategories());    // parent 선택용
        return "admin/categories/new";
    }

    // 카테고리 생성 처리
    @PostMapping
    public String create(@ModelAttribute("form") @Validated CategoryCreateForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model,
                         HttpSession session) {

        requireAdmin(session);

        if(bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getTopCategories());   // 상위 카테고리 목록
            return "admin/categories/new";
        }

        try {
            Long createdId = categoryService.create(form.getName(), form.getParentId());

            ra.addFlashAttribute("success", "카테고리가 생성되었습니다.");
            ra.addAttribute("id", createdId);
            return "redirect:/admin/categories/{id}/edit";
        } catch (ConflictException | IllegalArgumentException e) {
            model.addAttribute("categories", categoryService.getTopCategories());
            model.addAttribute("error", e.getMessage());
            return "admin/categories/new";
        }

    }

    // 카테고리 수정 폼
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           RedirectAttributes ra,
                           HttpSession session) {

        requireAdmin(session);

        try {
            AdminCategoryEditView category = categoryService.getAdminCategoryEditView(id);
            CategoryEditForm form = CategoryEditForm.of(category.getName(), category.getParentId());

            String parentName = null;
            if(category.getParentId() != null) {
                Category parent = categoryService.getAdmin(category.getParentId());
                parentName = parent.getName();
            }

            model.addAttribute("categoryId", category.getId());
            model.addAttribute("categoryName", category.getName());
            model.addAttribute("categorySlug", category.getSlug());
            model.addAttribute("currentParentName", parentName);

            model.addAttribute("form", form);
            model.addAttribute("categories", categoryService.getTopCategories());

            return "admin/categories/edit";
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categories";
        }

    }

    // 카테고리 수정 처리
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CategoryEditForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model,
                         HttpSession session) {

        requireAdmin(session);

        if(bindingResult.hasErrors()) {
            AdminCategoryEditView category = categoryService.getAdminCategoryEditView(id);

//            model.addAttribute("category", category);
            model.addAttribute("categoryId", category.getId());
            model.addAttribute("categories", categoryService.getTopCategories());
            return "admin/categories/edit";
        }

        try {
            categoryService.update(id, form.getName(), form.getParentId());

            ra.addFlashAttribute("success", "카테고리가 수정되었습니다.");
            return "redirect:/admin/categories/{id}/edit";
        } catch (ConflictException | IllegalArgumentException e) {
            AdminCategoryEditView category = categoryService.getAdminCategoryEditView(id);

//            model.addAttribute("category", category);
            model.addAttribute("categoryId", category.getId());
            model.addAttribute("categories", categoryService.getTopCategories());
            model.addAttribute("error", e.getMessage());
            return "admin/categories/edit";
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categories";
        }

    }

    // 카테고리 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String returnUrl,
                         RedirectAttributes ra,
                         HttpSession session) {

        requireAdmin(session);

        // delete()에 returnUrl 파라미터 받고 성공/실패 시 모두 그 위치로 돌려보내기
        String redirectUrl = resolveDeleteRedirectUrl(returnUrl);

        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "카테고리가 삭제되었습니다.");
            return "redirect:/admin/categories";    // 삭제 성공 시 목록으로 이동
        } catch (ConflictException | NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + redirectUrl;
        }
    }

    private String resolveDeleteRedirectUrl(String returnUrl) {
        if(returnUrl == null || returnUrl.trim().isBlank()) {
            return "/admin/categories";
        }

        // 아주 최소한의 오픈 리다이렉트 방지
        if(!returnUrl.startsWith("/")) {
            return "/admin/categories";
        }

        return returnUrl;
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
