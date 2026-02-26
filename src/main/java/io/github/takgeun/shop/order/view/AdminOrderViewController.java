package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.order.application.AdminOrderService;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.request.AdminOrderUpdateStatusRequest;
import io.github.takgeun.shop.order.dto.response.AdminOrderDetailResponse;
import io.github.takgeun.shop.order.view.form.OrderStatusForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderViewController {

    private final AdminOrderService adminOrderService;

    /**
     * 관리자 주문 목록
     * GET /admin/orders
     * TODO(확장) : status, keyword(회원/상품), 기간 필터 등 추가 기능
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", adminOrderService.getAll());
        return "admin/orders/list";
    }

    /**
     * 관리자 주문 상세
     * GET /admin/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public String detail(@PathVariable @NotNull @Positive Long orderId,
                         Model model,
                         RedirectAttributes ra) {
        try {
            AdminOrderDetailResponse order = adminOrderService.getDetailForAdmin(orderId);

            OrderStatusForm statusForm = new OrderStatusForm();
            statusForm.setStatus(order.getStatus());        // 현재 상태로 초기화

            model.addAttribute("order", order);
            model.addAttribute("statusForm", statusForm);
            model.addAttribute("statuses", OrderStatus.values());   // select 옵션용

            return "admin/orders/detail";
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    /**
     * 관리자 주문 상태 변경 (PRG)
     * POST /admin/orders/{orderId}/status
     */
    @PostMapping("/{orderId}/status")
    public String changeStatus(@PathVariable @NotNull @Positive Long orderId,
                               @Valid @ModelAttribute("statusForm") OrderStatusForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes ra) {

        log.info("orderId={}, received status={}", orderId, form.getStatus());

        // 폼 검증 실패 --> 같은 화면 forward (에러 표시)
        if(bindingResult.hasErrors()) {
            // 검증 오류 시에도 같은 화면 forward하면서 기존에 있던 정보도 그대로 보이게끔 해야함.
            try {
                AdminOrderDetailResponse order = adminOrderService.getDetailForAdmin(orderId);

                model.addAttribute("order", order);
                model.addAttribute("statuses", OrderStatus.values());
                return "admin/orders/detail";
            } catch (NotFoundException e) {
                ra.addFlashAttribute("error", e.getMessage());
                return "redirect:/admin/orders";
            }
        }

        // 실제 변경
        try {
            adminOrderService.changeStatus(orderId, form.getStatus());
            ra.addFlashAttribute("success", "주문 상태가 변경되었습니다.");
            return "redirect:/admin/orders/" + orderId;     // PRG
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders";
        } catch (ConflictException | IllegalArgumentException e) {
            // order.changeStatus()에서 발생되는 예외 처리
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders/" + orderId;
        }
    }
}
