package io.github.takgeun.shop.cart.application;

import io.github.takgeun.shop.cart.application.dto.CartItemResult;
import io.github.takgeun.shop.cart.application.dto.CartResult;
import io.github.takgeun.shop.cart.application.dto.CartSummaryResult;
import io.github.takgeun.shop.cart.domain.CartRepository;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int FREE_SHIPPING_THRESHOLD = 30_000;
    private static final int SHIPPING_FEE = 3_000;

    private final CartRepository cartRepository;
    private final ProductService productService;

    /**
     * 주문 생성용 최소 데이터
     */
    public List<CheckoutItemCommand> getCheckoutItems(HttpSession session) {

        validateSession(session);

        Map<Long, Integer> cart = cartRepository.findAll(session);

        if (cart == null || cart.isEmpty()) {
            return List.of();
        }

        return cart.entrySet()
                .stream()
                .map(entry -> {
                    validateProductId(entry.getKey());
                    validatePositiveQuantity(entry.getValue());

                    return new CheckoutItemCommand(
                            entry.getKey(),
                            entry.getValue()
                    );
                })
                .toList();
    }

    /**
     * 화면 렌더링용 카트 뷰
     */
    public CartResult getCart(HttpSession session) {
        validateSession(session);

        // [productId, qty]
        Map<Long, Integer> cart =
                cartRepository.findAll(session);

        if (cart == null || cart.isEmpty()) {
            return CartResult.empty();
        }

        List<CartItemResult> items = cart.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> {
                    Product product =
                            productService.getForOrder(entry.getKey());

                    return CartItemResult.from(
                            product,
                            entry.getValue()
                    );
                })
                .sorted(Comparator.comparing(
                        CartItemResult::productId
                ))
                .toList();

        int subtotal = items.stream()
                .mapToInt(CartItemResult::lineTotal)
                .sum();

        int discountTotal = items.stream()
                .mapToInt(CartItemResult::discountAmount)
                .sum();

        int shippingFee =
                subtotal >= FREE_SHIPPING_THRESHOLD
                        ? 0
                        : SHIPPING_FEE;

        CartSummaryResult summary =
                CartSummaryResult.of(
                        subtotal,
                        discountTotal,
                        shippingFee
                );

        return new CartResult(items, summary);
    }

    /**
     * 장바구니에 상품 추가
     * 이미 담긴 상품이면 기존 수량 + 요청 수량으로 누적시키기
     */
    public void add(HttpSession session, Long productId, int quantity) {

        validateSession(session);
        validateProductId(productId);
        validateAddQuantity(quantity); // 요청 수량 검증

        // .getForOrder()에서 NotFound, ON_SALE 검증
        Product product = productService.getForOrder(productId);

        Map<Long, Integer> cart = cartRepository.findAll(session);      // 해당 세션에 기존에 있는 카트 아이템 넣어놓기.
        int currentQty =
                (cart == null)
                        ? 0
                        : cart.getOrDefault(productId, 0);

        int nextQty = currentQty + quantity;         // 장바구니에 해당 상품이 추가된 이후의 최종 수량 (현재 장바구니 수량 + 새로 추가하려는 수량)

        validateStock(product, nextQty);     // 카트에 최종적으로 담기 전에 장바구니 최종 수량 기준 검증

        // 위 모든 기준 통과했으면 비로소 카트에 담기
        cartRepository.put(session, productId, nextQty);
    }

    /**
     * 장바구니 수량 변경 (+1 / -1)
     */
    public void changeQuantity(HttpSession session, Long productId, int delta) {
        validateSession(session);
        validateProductId(productId);

        if (delta == 0) {
            return;
        }

        Map<Long, Integer> cart = cartRepository.findAll(session);          // 해당 세션에 담긴 카트정보 불러오기
        if (cart == null || cart.isEmpty()) {
            return;
        }

        int currentQty = cart.getOrDefault(productId, 0);
        if (currentQty <= 0) {
            return;
        }

        int nextQty = currentQty + delta;

        if (nextQty < 1) {
            // 장바구니에 담긴 수량이 1보다 작아지면 카트에서 상품 제거
            cartRepository.remove(session, productId);
            return;
        }

        Product product = productService.getForOrder(productId);
        validateStock(product, nextQty);

        cartRepository.put(session, productId, nextQty);
    }

    /**
     * 장바구니 수량을 특정 값으로 직접 변경
     * 장바구니 페이지에서 input number 같은 UI가 있을 때 사용하기
     */
    public void updateQuantity(
            HttpSession session,
            Long productId,
            int quantity
    ) {
        validateSession(session);
        validateProductId(productId);
        validateUpdateQuantity(quantity);

        Map<Long, Integer> cart = cartRepository.findAll(session);

        /**
         * 세션·상품 ID·수량 형식 검증
         * → 장바구니에 상품이 존재하는지 확인
         * → 수량 0이면 제거 후 종료
         * → 수량 1 이상이면 상품 판매 상태와 재고 확인
         * → 수량 변경
         */
        if(cart == null || !cart.containsKey(productId)) {
            throw new NotFoundException(
                    "장바구니에 해당 상품이 없습니다."
            );
        }

        // 수량 0은 장바구니에서 제거
        if(quantity == 0) {
            cartRepository.remove(session, productId);
            return;
        }

        Product product = productService.getForOrder(productId);

        validateStock(product, quantity);

        cartRepository.put(session, productId, quantity);
    }


    public void remove(HttpSession session, Long productId) {
        validateSession(session);
        validateProductId(productId);

        Map<Long, Integer> cart = cartRepository.findAll(session);
        if (cart == null || cart.isEmpty()) {
            return;
        }

        cartRepository.remove(session, productId);
    }

    public void clear(HttpSession session) {
        validateSession(session);
        cartRepository.clear(session);
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId는 양수여야 합니다.");
        }
    }

    private void validateSession(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
    }

    private void validateAddQuantity(int quantity) {
        if(quantity < 1) {
            throw new IllegalArgumentException(
                    "수량은 1 이상이어야 합니다."
            );
        }
    }

    private void validateUpdateQuantity(int quantity) {
        if(quantity < 0) {
            throw new IllegalArgumentException(
                    "수량은 0 이상이어야 합니다."
            );
        }
    }

    private void validatePositiveQuantity(int quantity) {
        if(quantity < 1) {
            throw new IllegalArgumentException(
                    "수량은 1 이상이어야 합니다."
            );
        }
    }

    private void validateStock(
            Product product,
            int quantity
    ) {
        if(product.getStock() < quantity) {
            throw new ConflictException(
                    "장바구니 수량이 상품 재고보다 많습니다. 현재 재고: "
                    + product.getStock()
            );
        }
    }
}
