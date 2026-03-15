package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.CheckoutValidationSequence;
import io.github.takgeun.shop.order.application.OrderCheckoutService;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.view.dto.CheckoutItemView;
import io.github.takgeun.shop.order.view.dto.OrderCompleteView;
import io.github.takgeun.shop.order.view.dto.OrderHistoryPageView;
import io.github.takgeun.shop.order.view.form.CheckoutForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 이 경로는 인터셉터측에서 로그인 체크해줌
 */
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
    public String checkout(HttpServletRequest request,
                           Model model,
                           RedirectAttributes ra
    ) {

        // 인터셉터에서 이미 막아주고 있긴 하나 방어적 코딩
        // 로그인 안한 사용자가 접근하면 새 세션을 생성하게 되면 불필요한 세션이 증가하니 생성하지 않게 막기
        HttpSession session = request.getSession(false);

        Long memberId = getRequiredLoginMemberId(session);

        CartViewResult cartView = getCartViewOrEmpty(session);
        if(cartView.getItems().isEmpty()) {
            // 빈 카트면 checkout 페이지 대신 cart로 보내기
            ra.addFlashAttribute("error", "장바구니가 비어있습니다.");
            return "redirect:/cart";
        }

        attachCartModel(model, cartView);

        if(!model.containsAttribute("checkoutForm")) {
            CheckoutForm form = new CheckoutForm();
            form.setRequestKey(memberId + ":" + UUID.randomUUID());
            model.addAttribute("checkoutForm", form);
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
        HttpSession session = request.getSession(false);

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
     * 비즈니스 충돌(재고 부족 등) -> checkout 화면 재렌더링
     * 성공 --> /orders/{orderId} 로 redirect
     */
    @PostMapping
    public String createOrder(@Validated(CheckoutValidationSequence.class) @ModelAttribute("checkoutForm") CheckoutForm form,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes ra) {

        HttpSession session = request.getSession(false);    // 방어적 코딩
        Long memberId = getRequiredLoginMemberId(session);

        CartViewResult cartView = getCartViewOrEmpty(session);
        if(cartView.getItems().isEmpty()) {
            ra.addFlashAttribute("error", "장바구니가 비어있습니다.");
            return "redirect:/cart";        // 장바구니로 리다이렉트
        }

        // 폼 검증
        if(bindingResult.hasErrors()) {
            attachCartModel(model, cartView);       // 폼 검증 실패 시 장바구니에 담아놨던 아이템 뷰 정보를 모델에 저장
            return "public/orders/checkout";        // 같은 요청 안에서 model 넘겨주기 (같은 요청은 model 유지됨) -> 리다이렉트해버리면 model 사라짐 + BindingResult 사라짐 -> 폼 에러 표시 불가
        }

        // 주문 생성용 DTO (서비스로 넘길 때 쓰기)
        CreateOrderCommand cmd = new CreateOrderCommand(
                form.getRecipientName(),
                form.getPhoneNumber(),
                form.getZipCode(),
                form.getAddress(),
                form.getAddressDetail(),
                form.getRequestMessage(),
                form.getRequestKey()
        );

        try {
            Long orderId = orderCheckoutService.createOrderFromCart(memberId, session, cmd);
            return "redirect:/orders/" + orderId + "/complete";       // 주문완료 페이지로 리다이렉트
        } catch (ConflictException e) {
            // 사용자에게 checkout 화면에서 안내 가능한 예외만 캐치 (그 외 예외는 여기서 잡지 않음. 컨트롤러나 인터셉터 등 다른 데서 잡으니까)
            // 재고 부족
            // 판매 중이 아닌 상품
            // 주문 수량 이상
            // 주문 가능한 상품 없음
            log.warn("주문 생성 실패: memberId={}, message={}", memberId, e.getMessage());

            // 해당 세션의 주문 정보(아이템들, 서머리)를 모델에 담고 체크아웃 에러와 함께 제자리 포워딩
            CartViewResult lastestCartView = getCartViewOrEmpty(session);
            attachCartModel(model, lastestCartView);
            model.addAttribute("checkoutError", e.getMessage());
            return "public/orders/checkout";
        }
    }

    /**
     * 주문 완료 페이지
     * GET /orders/{orderId}/complete
     */
    @GetMapping("/{orderId:\\d+}/complete")
    public String complete(@PathVariable Long orderId,
                           HttpServletRequest request,
                           RedirectAttributes ra,
                           Model model) {

        HttpSession session = request.getSession(false);
        Long memberId = getRequiredLoginMemberId(session);

        OrderCompleteView view = orderCheckoutService.getOrderCompleteView(session, memberId, orderId);

        model.addAttribute("order", view);
        return "public/orders/complete";
    }


    // -------------------------------------------------------------------------------------------------------------


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

    private Long getRequiredLoginMemberId(HttpSession session) {
        if(session == null) {
            throw new IllegalArgumentException("로그인 세션이 존재하지 않습니다. 인터셉터 설정을 확인해주세요.");
        }

        Object idObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        if(!(idObj instanceof Long memberId)) {
            throw new IllegalArgumentException("로그인 회원 정보가 세션에 없습니다. 인터셉터 또는 로그인 처리를 확인해주세요.");
        }

        return memberId;
    }
}
