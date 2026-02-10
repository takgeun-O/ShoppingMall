package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.dto.request.OrderCreateRequest;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
//@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderViewController {

    private final ProductService productService;
    private final OrderService orderService;

    /**
     * 주문서 페이지
     * GET /orders/new?productId=~~
     */
    @GetMapping("/new")
    public String newOrderForm(
            @RequestParam @NotNull @Positive Long productId,
            Model model
    ) {
        Product product = productService.get(productId);

        // 폼 초깃값
        OrderCreateRequest form = new OrderCreateRequest(productId);
        model.addAttribute("form", form);

        // 화면 표시용 상품 정보
        model.addAttribute("product", product);

        return "orders/new";
    }

    /**
     * 주문 생성 처리 (PRG)
     * POST /orders
     */
    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") OrderCreateRequest form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        log.info("POST /orders 컨트롤러 진입");
        log.info("form={}", form);
        log.info("productId={}, qty={}, name={}, phone={}, zipCode={}, address={}, requestMessage={}",
                form.getProductId(), form.getQuantity(), form.getRecipientName(), form.getRecipientPhone(),
                form.getShippingZipCode(), form.getShippingAddress(), form.getRequestMessage());

        Product product = productService.get(form.getProductId());
        log.info("productStatus={}", product.getStatus());
        model.addAttribute("product", product);

        if(bindingResult.hasErrors()) {
            return "orders/new";
        }

        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        log.info("memberId={}", memberId);

        // 로그인 여부 확인
        if(memberId == null) {
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            ra.addAttribute("productId", form.getProductId());
            return "redirect:/orders/new";      // 로그인 뷰가 없으니 임시로 주문페이지로 돌아오도록 하기
//            return "redirect:/login";         // 추후 로그인 뷰 만들면 이거 쓰기
        }

        // 서비스 호출
        Long orderId = orderService.create(
                memberId, form.getProductId(), form.getQuantity(), form.getRecipientName(),
                form.getRecipientPhone(), form.getShippingZipCode(), form.getShippingAddress(),
                form.getRequestMessage()
        );

        if(orderId == null || orderId <= 0) {
            ra.addFlashAttribute("error", "주문 생성에 실패했습니다.");
            return "redirect:/orders";
        }

        // 성공메시지
        ra.addFlashAttribute("success", "주문이 완료되었습니다.");

        // PRG redirect : 내 주문 상세로 리다이렉트
        return "redirect:/orders/" + orderId;
    }

    /**
     * 내 주문 목록
     * GET /orders
     */
    @GetMapping
    public String myOrders(HttpSession session, Model model) {
        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        List<Order> orders = orderService.getMyOrders(memberId);

        model.addAttribute("orders", orders);

        return "orders/list";
    }

    /**
     * 내 주문 상세
     * GET /orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public String detail(
            @PathVariable @NotNull @Positive Long orderId,
            HttpSession session,
            Model model
    ) {

        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        OrderResponse order = orderService.getDetail(memberId, orderId);

        model.addAttribute("order", order);

        return "orders/detail";
    }

    /**
     * 주문 취소 (PRG)
     * POST /orders/{orderId}/cancel
     */
    @PostMapping("/{orderId}/cancel")
    public String cancel(
            @PathVariable @NotNull @Positive Long orderId,
            HttpSession session,
            RedirectAttributes ra
    ) {
        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        orderService.cancel(memberId, orderId);

        ra.addFlashAttribute("success", "주문이 취소되었습니다.");

        return "redirect:/orders/" + orderId;
    }
}
