package io.github.takgeun.shop.cart.application;

import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartItemView;
import io.github.takgeun.shop.cart.view.dto.CartSummaryView;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
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
     * 주문 생성용 최소 데이터
     */
    public List<CheckoutItem> getCheckoutItems(HttpSession session) {
        Map<Long, Integer> cart = cartRepository.findAll(session);

        if(cart == null || cart.isEmpty()) return List.of();

        return cart.entrySet().stream()
                .map(e -> CheckoutItem.of(e.getKey(), e.getValue()))
                .filter(i -> i.getQuantity() > 0)
                .toList();
    }

    /**
     * 화면 렌더링용 카트 뷰
     */
    public CartViewResult getCartView(HttpSession session) {
        Map<Long, Integer> cart = cartRepository.findAll(session);

        if(cart == null || cart.isEmpty()) {
            return CartViewResult.empty();
        }

        List<CartItemView> items = cart.entrySet().stream()
                .map(entry -> {
                    Long productId = entry.getKey();
                    int quantity = entry.getValue();

                    Product product = productService.getForOrderPublic(productId);
                    if(product == null) {
                        throw new NotFoundException("상품을 찾을 수 없습니다.");
                    }

                    return CartItemView.of(
                            product.getId(),
                            product.getName(),
                            product.getPrice(),
                            product.getOriginalPrice(),
                            quantity,
                            product.getImageUrl()
                    );
                })
                .filter(i -> i.getQuantity() > 0)
                .sorted(Comparator.comparing(CartItemView::getProductId))
                .toList();

        int subtotal = items.stream()
                .mapToInt(CartItemView::lineTotal)      // price * qty
                .sum();

        int discountTotal = items.stream()
                .mapToInt(this::discountAmount)         // (original - price) * qty
                .sum();

        int payableSubtotal = Math.max(subtotal - discountTotal, 0);        // 할인 적용 후 상품금액
        int shippingFee = payableSubtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;


        CartSummaryView summary = CartSummaryView.of(subtotal, discountTotal, shippingFee);

        return CartViewResult.of(items, summary);
    }

    public void add(HttpSession session, Long productId, int quantity) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }

        if(productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId는 양수여야 합니다.");
        }

        int resolvedQty = Math.max(quantity, 1);

        // 존재/판매 가능 상품 검증
        Product product = productService.getForOrderPublic(productId);
        if(product == null) {
            throw new NotFoundException("상품을 찾을 수 없습니다.");
        }

        // 현재 카트 가져오기
        Map<Long, Integer> cart = cartRepository.findAll(session);

        if(cart == null) {
            cart = new java.util.HashMap<>();
        }

        int currentQty = cart.getOrDefault(productId, 0);
        int nextQty = currentQty + resolvedQty;

        if(nextQty < 1) {
            nextQty = 1;
        }

        cartRepository.put(session, productId, nextQty);
    }

    public void changeQuantity(HttpSession session, Long productId, int delta) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
        if(productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId는 양수여야 합니다.");
        }
        if(delta == 0) {
            return;
        }

        Map<Long, Integer> cart = cartRepository.findAll(session);
        if(cart == null || cart.isEmpty()) {
            return; // 장바구니가 비어있으면 변화 없음.
        }

        int currentQty = cart.getOrDefault(productId, 0);
        if(currentQty <= 0) {
            return; // 없는 상품이면 변화 없음.
        }

        int nextQty = currentQty + delta;

        // 1 미만이면 삭제 처리
        if(nextQty < 1) {
            cartRepository.remove(session, productId);
            return;
        }

        cartRepository.put(session, productId, nextQty);
    }

    public void remove(HttpSession session, Long productId) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
        if(productId == null || productId <=0) {
            throw new IllegalArgumentException("productId는 양수여야 합니다.");
        }

        Map<Long, Integer> cart = cartRepository.findAll(session);
        if(cart == null || cart.isEmpty()) {
            return;
        }

        cartRepository.remove(session, productId);
    }

    public void clear(HttpSession session) {
        cartRepository.clear(session);
    }


    /**
     * 한 줄(상품 1종류)의 할인 금액
     * originalPrice가 없거나 price <= original 이 아니면 0
     */
    private int discountAmount(CartItemView item) {
        Integer original = item.getOriginalPrice();
        if (original == null) return 0;     // 정가가 없으면 판매가가 곧 정가

        int unitDiscount = original - item.getUnitPrice();
        if(unitDiscount <= 0) return 0;

        return unitDiscount * item.getQuantity();
    }
}
