package io.github.takgeun.shop.cart.application;

import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartItemView;
import io.github.takgeun.shop.cart.view.dto.CartSummaryView;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int FREE_SHIPPING_THRESHOLD = 30_000;
    private static final int SHIPPING_FEE = 3_000;

    private final SessionCartRepository cartRepository;
    private final ProductService productService;

    /**
     * 장바구니 조회 (템플릿에 바로 들어갈 View 모델)
     */
    public CartViewResult getCartView(HttpSession session) {
        Map<Long, Integer> cart = cartRepository.findAll(session);      // [productId, quantity]

        List<CartItemView> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : cart.entrySet()) {
            Long productId = e.getKey();
            int qty = e.getValue();

            Product p = getProductOrThrow(productId);

            // 이미지 URL은 임시로 기본 이미지로 처리
            String imageUrl = "/images/no-image.png";

            items.add(CartItemView.of(
                    productId,
                    p.getName(),
                    p.getPrice(),
                    qty,
                    imageUrl
            ));
        }

        // 일단 id순 정렬 --> id순 자체가 카트에 가장 먼저 넣은 상품이 위로 올라가게 되어있음.
        items.sort(Comparator.comparing(CartItemView::getId));

        int subtotal = items.stream()
                .mapToInt(CartItemView::lineTotal)
                .sum();
        int shippingFee = (subtotal >= FREE_SHIPPING_THRESHOLD || subtotal == 0) ? 0 : SHIPPING_FEE;

        CartSummaryView summary = CartSummaryView.of(subtotal, shippingFee);
        return CartViewResult.from(items, summary);
    }

    /**
     * 담기 (이미 담긴 상품일 경우 수량만 증가)
     */
    public void add(HttpSession session, Long productId, int quantity) {
        if(quantity <= 0) return;

        // 상품 존재 검증
        getProductOrThrow(productId);

        int currentQuantity = cartRepository.getQuantity(session, productId);
        cartRepository.put(session, productId, currentQuantity + quantity);
    }

    /**
     * 수량 변경 (delta: +1 / -1)
     * 1 미만이면 삭제 처리
     */
    public void changeQuantity(HttpSession session, Long productId, int delta) {

        // 상품 존재 검증
        getProductOrThrow(productId);

        int currentQuantity = cartRepository.getQuantity(session, productId);
        int nextQuantity = currentQuantity + delta;

        if(nextQuantity <= 0) {
            cartRepository.remove(session, productId);
        } else {
            cartRepository.put(session, productId, nextQuantity);
        }
    }

    public void remove(HttpSession session, Long productId) {
        cartRepository.remove(session, productId);
    }

    public void clear(HttpSession session) {
        cartRepository.clear(session);
    }

    private Product getProductOrThrow(Long productId) {
        try {
            return productService.getForOrderPublic(productId);
        } catch (Exception e) {
            throw new NotFoundException("존재하지 않는 상품입니다.");
        }
    }
}
