package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.view.dto.admin.AdminProductListItemView;
import io.github.takgeun.shop.product.view.dto.admin.AdminProductSummaryView;
import io.github.takgeun.shop.product.view.dto.ProductDetailView;
import io.github.takgeun.shop.product.view.form.ProductCreateForm;
import io.github.takgeun.shop.product.view.form.ProductUpdateForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class AdminProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 관리자 상품 관리 페이지
     * GET /admin/products
     */
    @GetMapping
    public String list(Model model, HttpSession session) {

        requireAdmin(session);

        log.info("관리자 상품 관리 페이지 진입");

        List<Product> products = productService.findAdminList(null, "latest");

        List<AdminProductListItemView> views = products.stream()
                .map(this::toAdminListItemView)
                .toList();

        AdminProductSummaryView summary = AdminProductSummaryView.of(views);

        model.addAttribute("products", views);
        model.addAttribute("summary", summary);
        model.addAttribute("treeMode", "admin");
        model.addAttribute("isAdmin", true);

        return "admin/products/list";
    }

    /**
     * 상품 상세 페이지 (관리자 전용)
     * GET /admin/products/{productId}
     */
    @GetMapping("/{productId:\\d+}")
    public String detail(
            @PathVariable @Positive Long productId,
            Model model,
            HttpSession session, HttpServletRequest request) {

        requireAdmin(session);

        Product product = productService.getAdminDetail(productId);
        ProductDetailView view = ProductDetailView.from(product);

        String returnUrl = request.getRequestURI();
        String query = request.getQueryString();
        if(query != null) {
            returnUrl += "?" + query;
        }

        model.addAttribute("product", view);
        model.addAttribute("treeMode", "public");
        model.addAttribute("returnUrl", returnUrl);

        return "admin/products/detail";
    }

    /**
     * 상품 등록 폼
     * GET /admin/products/new
     */
    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        requireAdmin(session);

        if(!model.containsAttribute("form")) {
            ProductCreateForm form = new ProductCreateForm();
            form.setStatus(ProductStatus.READY);
            model.addAttribute("form", form);
        }

        // categories / productStatuses / treeMode / isAdmin
        populateProductFormModel(model);

        return "admin/products/new";
    }

    /**
     * 상품 등록 처리
     * POST /admin/products
     */
    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") ProductCreateForm form,
            BindingResult bindingResult,
            Model model,
            HttpSession session
    ) {
        requireAdmin(session);

        validatePriceRelation(form.getPrice(), form.getOriginalPrice(), bindingResult);

        if(bindingResult.hasErrors()) {
            // 에러 났을 때 제자리 포워딩하는 동시에 정보 그대로 옮기기
            populateProductFormModel(model);
            return "admin/products/new";
        }

        try {
            Long productId = productService.create(
                    form.getCategoryId(),
                    form.getName(),
                    form.getPrice(),
                    form.getStock(),
                    form.getDescription(),
                    form.getStatus(),
                    form.getOriginalPrice(),
                    form.getImageUrl()
            );

            log.info("상품 등록 완료, productId={}", productId);
            return "redirect:/admin/products/" + productId;
        } catch (IllegalArgumentException e) {
            bindingResult.reject("createFail", e.getMessage());
            populateProductFormModel(model);
            return "admin/products/new";
        }
    }

    /**
     * 상품 수정 폼
     * GET /admin/products/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable("id") @Positive Long productId,
            Model model,
            HttpSession session
    ) {
        requireAdmin(session);

        Product product = productService.getAdminDetail(productId);

        if(!model.containsAttribute("form")) {
            model.addAttribute("form", ProductUpdateForm.from(product));
        }

        model.addAttribute("productId", productId);
        populateProductFormModel(model);

        return "admin/products/edit";
    }

    /**
     * 상품 수정 처리
     * POST /admin/products/{id}/edit
     */
    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable("id") @Positive Long productId,
            @Valid @ModelAttribute("form") ProductUpdateForm form,
            BindingResult bindingResult,
            Model model,
            HttpSession session,
            RedirectAttributes ra
    ) {
        requireAdmin(session);

        validatePriceRelation(form.getPrice(), form.getOriginalPrice(), bindingResult);

        if(bindingResult.hasErrors()) {
            model.addAttribute("productId", productId);
            populateProductFormModel(model);
            return "admin/products/edit";
        }

        try {
            productService.update(
                    productId,
                    form.getCategoryId(),
                    form.getName(),
                    form.getPrice(),
                    form.getStock(),
                    form.getDescription(),
                    form.getStatus(),
                    form.getOriginalPrice(),
                    form.getImageUrl()
            );

            log.info("상품 수정 완료. productId={}", productId);
            ra.addFlashAttribute("success", "상품 수정이 완료되었습니다.");
            return "redirect:/admin/products";
        } catch (NotFoundException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            bindingResult.reject("updateFail", e.getMessage());     // 글로벌 에러
            model.addAttribute("productId", productId);
            populateProductFormModel(model);
            return "admin/products/edit";
        } catch (ConflictException e) {
            // 상태 변경 ConflictException 잡기용
            bindingResult.reject("updateFail", e.getMessage());
            model.addAttribute("productId", productId);
            populateProductFormModel(model);
            return "admin/products/edit";
        }
    }

    /**
     * 상품 삭제 처리
     * POST /admin/{id}/delete
     */
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long productId,
            HttpSession session,
            RedirectAttributes ra
    ) {
        requireAdmin(session);

        try {
            productService.delete(productId);   // NotFound, IllegalState (이건 ControllerAdvice로 전역 처리), Conflict
            log.info("상품 삭제 완료. productId={}", productId);
            ra.addFlashAttribute("success", "상품이 삭제되었습니다.");
        } catch (NotFoundException e) {
            throw e;
        } catch (ConflictException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/products";
    }

    /**
     * 상품 숨김 처리
     * POST /admin/products/{id}/hide
     */
    @PostMapping("/{id}/hide")
    public String hide(
            @PathVariable("id") @Positive Long productId,
            HttpSession session,
            RedirectAttributes ra
    ) {
        requireAdmin(session);

        try {
            productService.changeStatus(productId, ProductStatus.HIDDEN);
            log.info("상품 숨김 처리. productId={}", productId);
            ra.addFlashAttribute("success", "상품을 숨김 처리했습니다.");
        } catch (NotFoundException e) {
            // 존재하지 않는 상품 처리
            throw e;
        } catch (ConflictException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/products";
    }

    /**
     * 상품 공개 처리
     * POST /admin/products/{id}/show
     */
    @PostMapping("/{id}/show")
    public String show(
            @PathVariable("id") @Positive Long productId,
            HttpSession session,
            RedirectAttributes ra
    ) {
        requireAdmin(session);

        try {
            productService.changeStatus(productId, ProductStatus.ON_SALE);
            log.info("상품 공개 처리. productId={}", productId);
            ra.addFlashAttribute("success", "상품을 판매중으로 변경했습니다.");
        } catch (NotFoundException e) {
            // 존재하지 않는 상품 처리
            throw e;
        } catch (ConflictException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/products";
    }


    private void requireAdmin(HttpSession session) {
        if (session == null) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if (!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
    }

    private void populateProductFormModel(Model model) {
        List<ProductStatus> editableStatuses = Arrays.stream(ProductStatus.values())
                        .filter(status -> status != ProductStatus.SOLD_OUT)
                                .toList();

        model.addAttribute("categories", categoryService.getAllAdminCategories());
        model.addAttribute("productStatuses", editableStatuses);

        model.addAttribute("treeMode", "admin");
        model.addAttribute("isAdmin", true);
    }

    private void validatePriceRelation(Integer price, Integer originalPrice, BindingResult bindingResult) {
        if(price == null || originalPrice == null) {
            return;
        }

        if(originalPrice < price) {
            bindingResult.rejectValue("originalPrice", "invalid.originalPrice", "정가는 판매가 이상이어야 합니다.");
        }
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) return "latest";
        return switch (sort) {
            case "latest", "best", "sale", "price-low", "price-high", "rating" -> sort;
            default -> "latest";
        };
    }

    private AdminProductListItemView toAdminListItemView(Product product) {
        String categoryName = categoryService.findAdminNameOrNull(product.getCategoryId());

        String imageUrl = product.getImageUrl();
        if(imageUrl == null || imageUrl.trim().isBlank()) {
            imageUrl = "/images/no-image.png";
        }

        return AdminProductListItemView.of(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                imageUrl,
                categoryName != null ? categoryName : "미분류",
                product.getStatus()
        );
    }
}
