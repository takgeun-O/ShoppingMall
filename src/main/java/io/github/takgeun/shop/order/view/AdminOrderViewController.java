package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.order.application.AdminOrderService;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.view.dto.admin.AdminOrderDetailView;
import io.github.takgeun.shop.order.view.form.OrderStatusForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Slf4j
@ViewController
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderViewController {

    private final AdminOrderService adminOrderService;

    /**
     * 관리자 주문 목록
     * GET /admin/orders
     */
    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "") String keyword,
                       @RequestParam(required = false, defaultValue = "ALL") String status,
                       Model model) {

        model.addAttribute("orders", adminOrderService.getOrderList(keyword, status));
        model.addAttribute("summary", adminOrderService.getOrderSummary());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

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
            AdminOrderDetailView order = adminOrderService.getDetailForAdmin(orderId);  // 회원, 주문 NotFound

            OrderStatusForm statusForm = new OrderStatusForm();
            statusForm.setStatus(order.getStatus());

            model.addAttribute("order", order);
            model.addAttribute("statusForm", statusForm);
            model.addAttribute("statuses", adminOrderService.getAvailableNextStatuses(orderId));

            return "admin/orders/detail";
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    /**
     * 관리자 주문 상태 변경
     * POST /admin/orders/{orderId}/status
     */
    @PostMapping("/{orderId}/status")
    public String changeStatus(@PathVariable @NotNull @Positive Long orderId,
                               @Valid @ModelAttribute("statusForm") OrderStatusForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes ra) {

        log.info("관리자 주문 상태 변경 시도 : orderId={}, status={}", orderId, form.getStatus());

        if(bindingResult.hasErrors()) {
            try {
                AdminOrderDetailView order = adminOrderService.getDetailForAdmin(orderId);
                model.addAttribute("order", order);
                model.addAttribute("statuses", OrderStatus.values());
                return "admin/orders/detail";
            } catch (NotFoundException e) {
                ra.addFlashAttribute("error", e.getMessage());
                return "redirect:/admin/orders";
            }
        }

        try {
            adminOrderService.changeStatus(orderId, form.getStatus());
            ra.addFlashAttribute("success", "주문 상태가 변경되었습니다.");
            return "redirect:/admin/orders/" + orderId;
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders";
        } catch (ConflictException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/orders/" + orderId;
        }
    }
}
