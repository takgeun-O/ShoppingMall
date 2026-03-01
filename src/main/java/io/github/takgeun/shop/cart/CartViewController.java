package io.github.takgeun.shop.cart;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.view.dto.CartItemView;
import io.github.takgeun.shop.cart.view.dto.CartSummaryView;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

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
    public String cart(HttpServletRequest request, Model model) {

        HttpSession session = request.getSession(true);     // 기존에 세션이 존재하면 그걸 반환하고 존재하지 않으면 새로 생성

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
                          HttpServletRequest request,
                          RedirectAttributes ra) {

        HttpSession session = request.getSession(true);

        // 1 이하 방지
        int resolvedQty = Math.max(quantity, 1);
        cartService.add(session, productId, resolvedQty);

        ra.addFlashAttribute("success", "장바구니에 담았습니다.");
        ra.addFlashAttribute("added", true);
        ra.addFlashAttribute("addedProductId", productId);
        ra.addFlashAttribute("addedQty", resolvedQty);

        if(returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/cart";
    }

    /**
     * 수량 변경
     * POST /cart/items/{id}/quantity(id=${item.id})
     */
    @PostMapping("/items/{id}/quantity")
    public String changeQuantity(
            @PathVariable Long id,
            @RequestParam int delta,
            HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        cartService.changeQuantity(session, id, delta);
        return "redirect:/cart";
    }

    /**
     * 단일 삭제
     * POST /cart/items/{id}/remove
     */
    @PostMapping("/items/{id}/remove")
    public String removeItem(@PathVariable Long id, HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        cartService.remove(session, id);
        return "redirect:/cart";
    }

    /**
     * 전체 비우기
     * POST /cart/clear
     */
    @PostMapping("/clear")
    public String clear(HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        cartService.clear(session);
        return "redirect:/cart";
    }
}
