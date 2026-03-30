package io.github.takgeun.shop.cart.view;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartViewController {

    private final CartService cartService;

    /**
     * 장바구니 조회
     * GET /cart
     */
    @GetMapping
    public String cart(HttpSession session, Model model) {

        CartViewResult view = cartService.getCartView(session);

        model.addAttribute("items", view.getItems());
        model.addAttribute("summary", view.getSummary());

        return "public/cart/index";
    }

    /**
     * 장바구니 담기
     * POST /cart/items
     */
    @PostMapping("/items")
    public String addItem(@RequestParam Long productId,
                          @RequestParam(defaultValue = "1") int quantity,
                          @RequestParam(required = false) String returnUrl,
                          HttpSession session,
                          RedirectAttributes ra) {

        // 1 이하 방지
        int resolvedQty = Math.max(quantity, 1);

        try {
            cartService.add(session, productId, resolvedQty);       // ConflictException, NotFoundException

            ra.addFlashAttribute("success", "장바구니에 담았습니다.");
            ra.addFlashAttribute("added", true);
            ra.addFlashAttribute("addedProductId", productId);
            ra.addFlashAttribute("addedQty", resolvedQty);
        } catch (ConflictException | IllegalArgumentException e) {
            // 에러 터지면 뷰에다가 그냥 에러 정보만 보내주고 끝.
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("errorProductId", productId);
        } catch (NotFoundException e) {
            // 장바구니에 담기 직전 상품이 삭제되었을 경우 그 자리에서 NotFoundException 메시지 보여주는 게 좋을 듯
            ra.addFlashAttribute("error", "해당 상품은 현재 판매 중이 아니거나 찾을 수 없습니다.");
            ra.addFlashAttribute("errorProductId", productId);
        }

        return "redirect:" + resolveReturnUrl(returnUrl, "/cart");
    }

    /**
     * 수량 변경
     * POST /cart/items/{id}/quantity(id=${item.id})
     */
    @PostMapping("/items/{id}/quantity")
    public String changeQuantity(
            @PathVariable Long id,
            @RequestParam int delta,
            HttpSession session,
            RedirectAttributes ra) {

        if(delta == 0) {
            return "redirect:/cart";
        }

        // 수량 변경은 장바구니 화면에서 사용자가 즉시 시도하는 동작이라서 예외는 해당 페이지로 다시 보내면서 표시해주는 게 자연스러움.
        try {
            cartService.changeQuantity(session, id, delta);     // ConflictException 잡아야함.
        } catch (IllegalArgumentException | ConflictException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("errorProductId", id);
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", "해당 상품은 현재 판매 중이 아니거나 찾을 수 없습니다.");
            ra.addFlashAttribute("errorProductId", id);
        }

        return "redirect:/cart";
    }

    /**
     * 단일 삭제
     * POST /cart/items/{id}/remove
     */
    @PostMapping("/items/{id}/remove")
    public String removeItem(@PathVariable Long id, HttpSession session) {

        cartService.remove(session, id);
        return "redirect:/cart";
    }

    /**
     * 전체 비우기
     * POST /cart/clear
     */
    @PostMapping("/clear")
    public String clear(HttpSession session) {

        cartService.clear(session);
        return "redirect:/cart";
    }

    /**
     * redirect 대상 URL 보안 처리
     * 내부 경로만 허용 ("/..." 로 시작)
     * 비어있거나 외부 URL이면 fallback
     */
    private String resolveReturnUrl(String returnUrl, String fallback) {
        if(returnUrl == null) {
            return fallback;
        }

        String trimmed = returnUrl.trim();
        if(trimmed.isEmpty()) {
            return fallback;
        }

        // 내부 경로만 허용 ("/products/1", "/cart" 등등)
        if(trimmed.startsWith("/")) {
            return trimmed;
        }
        return fallback;
    }
}
