package io.github.takgeun.shop.cart.api;

import io.github.takgeun.shop.cart.api.request.AddCartItemRequest;
import io.github.takgeun.shop.cart.api.request.UpdateCartItemQuantityRequest;
import io.github.takgeun.shop.cart.api.response.CartResponse;
import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.application.dto.CartResult;
import io.github.takgeun.shop.global.api.ApiController;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @ApiController 사용 이유
 * - @RestController를 대신하여 API 컨트롤러임을 명확하게 표현
 * - API 컨트롤러와 Thymeleaf 컨트롤러(ViewController)를 구분
 * - ApiGlobalExceptionHandler 같은 Advice 적용 범위를 안전하게 제한
 */
@ApiController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartApiController {

    private final CartService cartService;

    /**
     * 장바구니 조회
     * GET /api/v1/cart
     */
    @GetMapping
    public CartResponse getCart(HttpSession session) {
        CartResult result = cartService.getCart(session);

        return CartResponse.from(result);
    }

    /**
     * 장바구니에 상품 추가
     * POST /api/v1/cart/items
     *
     * 상품 추가는 이미 존재하는 상품의 수량을 누적하기도 하므로 항상 새로운 리소스를 생성한다고 보기 어려움
     * 따라서 201 Created보다 204 No Content가 자연스럽다.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addItem(
            HttpSession session,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        cartService.add(
                session,
                request.productId(),
                request.quantity()
        );
    }

    /**
     * 장바구니 상품 수량 변경
     * PATCH /api/v1/cart/items/{productId}
     */
    @PatchMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateItemQuantity(
            HttpSession session,
            @PathVariable @Positive Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        cartService.updateQuantity(
                session,
                productId,
                request.quantity()
        );
    }

    /**
     * 장바구니에서 단일 상품 삭제
     * DELETE /api/v1/cart/items/{productId}
     */
    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            HttpSession session,
            @PathVariable @Positive Long productId
    ) {
        cartService.remove(
                session,
                productId
        );
    }

    /**
     * 장바구니 전체 비우기
     * DELETE /api/v1/cart
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(
            HttpSession session
    ) {
        cartService.clear(session);
    }
}
