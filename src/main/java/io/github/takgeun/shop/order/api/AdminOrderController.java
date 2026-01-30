package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.order.application.AdminOrderService;
import io.github.takgeun.shop.order.dto.response.AdminOrderListResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<List<AdminOrderListResponse>> getAll(HttpSession session) {

        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        List<AdminOrderListResponse> response = adminOrderService.getAll(memberId);

        return ResponseEntity.ok(response);
    }
}
