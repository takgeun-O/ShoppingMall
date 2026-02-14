package io.github.takgeun.shop.category.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.view.form.CategoryCreateForm;
import io.github.takgeun.shop.category.view.form.CategoryEditForm;
import io.github.takgeun.shop.global.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class CategoryViewController {

    private final CategoryService categoryService;

    // public 카테고리 목록 조회
    @GetMapping("/categories")
    public String categories(Model model) {
        List<Category> categories = categoryService.getAllPublic();
        model.addAttribute("categories", categories);
        return "categories/list";
    }

    // admin 카테고리 목록 조회
    @GetMapping("/admin/categories")
    public String adminList(Model model) {
        List<Category> categories = categoryService.getAllAdmin();
        model.addAttribute("categories", categories);
        return "admin/categories/list";
    }

    // 카테고리 생성 폼
    @GetMapping("/admin/categories/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CategoryCreateForm());
        model.addAttribute("categories", categoryService.getAllAdmin());    // parent 선택용
        return "admin/categories/new";
    }

    // 카테고리 생성
    @PostMapping("/admin/categories")
    public String create(@ModelAttribute("form") @Validated CategoryCreateForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {

        if(bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllAdmin());
            return "admin/categories/new";
        }

        Long createdId = categoryService.create(form.getName(), form.getParentId());

        ra.addFlashAttribute("success", "카테고리가 생성되었습니다.");
        ra.addAttribute("id", createdId);
        return "redirect:/admin/categories/{id}/edit";
    }

    // 카테고리 수정 폼
    @GetMapping("/admin/categories/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getAdmin(id);

        CategoryEditForm form = new CategoryEditForm();
        form.setName(category.getName());
        form.setParentId(category.getParentId());   // parentId가 없으면 null

        model.addAttribute("categoryId", id);
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.getAllAdmin());    // parent 선택용
        return "admin/categories/edit";
    }

    // 카테고리 수정
    @PostMapping("/admin/categories/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") @Validated CategoryEditForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {

        if(bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            model.addAttribute("categories", categoryService.getAllAdmin());
            return "/admin/categories/edit";
        }

        categoryService.update(id, form.getName(), form.getParentId(), form.getStatus());
        ra.addFlashAttribute("success", "카테고리가 수정되었습니다.");
        return "redirect:/admin/categories/{id}/edit";
    }

    // 카테고리 삭제
    @PostMapping("/admin/categories/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "카테고리가 삭제되었습니다.");
            return "redirect:/admin/categories";
        } catch (ConflictException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categories/{id}/edit";
        }
    }
}
