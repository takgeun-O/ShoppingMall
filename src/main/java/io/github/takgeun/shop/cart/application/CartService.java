package io.github.takgeun.shop.cart.application;

import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartItemView;
import io.github.takgeun.shop.cart.view.dto.CartSummaryView;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
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

        if(cart == null || cart.isEmpty()) {
            return List.of();
        }

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
                    int quantity = entry.getValue();        // 카트에 담긴 수량

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

    /**
     * 장바구니에 상품 추가
     * 이미 담긴 상품이면 기존 수량 + 요청 수량으로 누적시키기
     */
    public void add(HttpSession session, Long productId, int quantity) {

        validateSession(session);
        validateProductId(productId);

        int resolvedQty = normalizeQuantity(quantity);      // 최소 주문 수량은 1

        Product product = getOrderableProduct(productId);

        Map<Long, Integer> cart = cartRepository.findAll(session);      // 해당 세션에 기존에 있는 카트 아이템 넣어놓기.
        int currentQty = (cart == null) ? 0 : cart.getOrDefault(productId, 0);
        int nextQty = currentQty + resolvedQty;         // 장바구니에 해당 상품이 추가된 이후의 최종 수량 (현재 장바구니 수량 + 새로 추가하려는 수량)

        validateCartQuantity(product, nextQty);     // 카트에 최종적으로 담기 전에 장바구니 최종 수량 기준 검증

        // 위 모든 기준 통과했으면 비로소 카트에 담기
        cartRepository.put(session, productId, nextQty);
    }

    /**
     * 장바구니 수량 변경 (+1 / -1)
     */
    public void changeQuantity(HttpSession session, Long productId, int delta) {
        validateSession(session);
        validateProductId(productId);

        if(delta == 0) {
            return;
        }

        Map<Long, Integer> cart = cartRepository.findAll(session);          // 해당 세션에 담긴 카트정보 불러오기
        if(cart == null || cart.isEmpty()) {
            return;
        }

        int currentQty = cart.getOrDefault(productId, 0);
        if(currentQty <= 0) {
            return;
        }

        int nextQty = currentQty + delta;

        if(nextQty < 1) {
            // 장바구니에 담긴 수량이 1보다 작아지면 카트에서 상품 제거
            cartRepository.remove(session, productId);
            return;
        }

        Product product = getOrderableProduct(productId);
        validateCartQuantity(product, nextQty);             // 주문 수량 검증 (0 이하라던가, 재고보다 많이 담았다던가)

        cartRepository.put(session, productId, nextQty);
    }

    /**
     * 장바구니 수량을 특정 값으로 직접 변경
     * 장바구니 페이지에서 input number 같은 UI가 있을 때 사용하기
     */
    public void updateQuantity(HttpSession session, Long productId, int quantity) {
        validateSession(session);
        validateProductId(productId);

        Map<Long, Integer> cart = cartRepository.findAll(session);
        if(cart == null || cart.isEmpty()) {
            return;
        }

        if(!cart.containsKey(productId)) {
            return;
        }

        if(quantity < 1) {
            cartRepository.remove(session, productId);
            return;
        }

        Product product = getOrderableProduct(productId);
        validateCartQuantity(product, quantity);

        cartRepository.put(session, productId, quantity);
    }


    public void remove(HttpSession session, Long productId) {
        validateSession(session);
        validateProductId(productId);

        Map<Long, Integer> cart = cartRepository.findAll(session);
        if(cart == null || cart.isEmpty()) {
            return;
        }

        cartRepository.remove(session, productId);
    }

    public void clear(HttpSession session) {
        validateSession(session);
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

    private void validateCartQuantity(Product product, int quantity) {
        if(quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        if(product.getStock() < quantity) {
            throw new ConflictException("주문 수량이 판매 중인 상품의 재고보다 많습니다. 현재 재고 : " + product.getStock());
        }
    }

    private Product getOrderableProduct(Long productId) {
        Product product = productService.getForOrderPublic(productId);      // 일반 사용자 주문용 상품 꺼내기
        if(product == null) {
            throw new NotFoundException("상품을 찾을 수 없습니다.");
        }

        if(product.getStatus() != ProductStatus.ON_SALE) {
            throw new ConflictException("판매 중인 상품만 장바구니에 담을 수 있습니다.");
        }

        return product;
    }

    private int normalizeQuantity(int quantity) {
        return Math.max(quantity, 1);
    }

    private void validateProductId(Long productId) {
        if(productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId는 양수여야 합니다.");
        }
    }

    private void validateSession(HttpSession session) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
    }
}
