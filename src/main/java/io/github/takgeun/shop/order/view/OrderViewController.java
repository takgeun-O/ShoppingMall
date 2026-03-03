package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.CheckoutValidationSequence;
import io.github.takgeun.shop.order.application.OrderCheckoutService;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.view.dto.CheckoutItemView;
import io.github.takgeun.shop.order.view.dto.OrderCompleteView;
import io.github.takgeun.shop.order.view.form.CheckoutForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderViewController {

    private final CartService cartService;
    private final OrderCheckoutService orderCheckoutService;

    /**
     * 주문/결제 페이지
     * GET /orders/checkout
     */
    @GetMapping("/checkout")
    public String checkout(HttpServletRequest request, Model model, RedirectAttributes ra) {
        HttpSession session = request.getSession(false);

        CartViewResult cartView = getCartViewOrEmpty(session);
        if(cartView.getItems().isEmpty()) {
            // 빈 카트면 checkout 페이지 대신 cart로 보내기
            ra.addFlashAttribute("error", "장바구니가 비어있습니다.");
            return "redirect:/cart";
        }

        attachCartModel(model, cartView);

        if(!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", new CheckoutForm());
        }

        return "public/orders/checkout";
    }

    /**
     * 주문 페이지에서 로그인이 풀렸을 때 재로그인해서 리다이렉트로 /orders에 들어올 경우
     * GET /orders 경로로 요청이 들어오는거라 매핑을 안하면 에러 발생
     * 이러한 상황을 막고자 안전장치용으로 만들음.
     * GET /orders 랜딩 시 장바구니로 보내기
     */
    @GetMapping
    public String orderRoot(HttpServletRequest request, RedirectAttributes ra) {
        HttpSession session = request.getSession(false);        // 기존 세션 없으면 null 반환
        CartViewResult cartView = (session == null) ? CartViewResult.empty() : cartService.getCartView(session);
        if(cartView.getItems().isEmpty()) {
            ra.addFlashAttribute("error", "장바구니가 비어있습니다.");
            return "redirect:/cart";
        }
        return "redirect:/orders/checkout";
    }

    /**
     * 결제하기(=주문 생성)
     * POST /orders
     *
     * 검증 실패 --> checkout 화면 재렌더링
     * 성공 --> /orders/{orderId} 로 redirect
     */
    @PostMapping
    public String createOrder(@Validated(CheckoutValidationSequence.class) @ModelAttribute("checkoutForm") CheckoutForm form,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes ra) {

        HttpSession session = request.getSession(false);
        if(session == null) {
            return "redirect:" + redirectToLogin("/orders/checkout");
        }

        Object idObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        if(!(idObj instanceof Long memberId)) {
            return "redirect:" + redirectToLogin("/orders/checkout");
        }

        CartViewResult cartView = cartService.getCartView(session);

        // 장바구니가 비었으면 checkout이 아닌 cart로 리다이렉트
        if(cartView.getItems().isEmpty()) {
            ra.addFlashAttribute("error", "장바구니가 비어있습니다.");
            return "redirect:/cart";
        }

        if(bindingResult.hasErrors()) {
            attachCartModel(model, cartView);
            return "public/orders/checkout";
        }

        // CreateOrderCommand : 주문 생성용 DTO (서비스로 넘길 것)
        CreateOrderCommand cmd = new CreateOrderCommand(
                form.getRecipientName(),
                form.getPhoneNumber(),
                form.getZipCode(),
                form.getAddress(),
                form.getAddressDetail(),
                form.getRequestMessage()
        );

        // 실제 결제 연동 전 MVP : 주문 생성 + 결제완료 처리
        Long orderId = orderCheckoutService.createOrderFromCart(memberId, session, cmd);

        return "redirect:/orders/" + orderId;
    }

    /**
     * 주문 완료 페이지
     * GET /orders/{orderId}
     */
    @GetMapping("/{orderId:\\d+}")
    public String complete(@PathVariable Long orderId,
                           HttpServletRequest request,
                           RedirectAttributes ra,
                           Model model) {

        HttpSession session = request.getSession(false);
        Long memberId = getLoginMemberId(session);
        if(memberId == null) {
            return "redirect:" + redirectToLogin("/orders/" + orderId);
        }

        OrderCompleteView view = orderCheckoutService.getOrderCompleteView(session, memberId, orderId);

        model.addAttribute("order", view);
        return "public/orders/complete";
    }

    private CartViewResult getCartViewOrEmpty(HttpSession session) {
        if(session == null) {
            return CartViewResult.empty();
        }
        return cartService.getCartView(session);
    }

    private void attachCartModel(Model model, CartViewResult cartView) {
        // checkout 전용 뷰 아이템으로 변환
        List<CheckoutItemView> checkoutItems = cartView.getItems().stream()
                .map(CheckoutItemView::from)
                .toList();

        model.addAttribute("items", checkoutItems);
        model.addAttribute("summary", cartView.getSummary());
    }

    private String redirectToLogin(String next) {
        return UriComponentsBuilder
                .fromPath("/login")
                .queryParam("reason", "LOGIN_REQUIRED")
                .queryParam("next", next)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private Long getLoginMemberId(HttpSession session) {
        if(session == null) return null;

        Object idObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        return (idObj instanceof  Long id) ? id : null;
    }
}
