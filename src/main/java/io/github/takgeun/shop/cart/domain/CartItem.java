package io.github.takgeun.shop.cart.domain;

import lombok.Getter;

/**
 * 현재 구현한 Cart는 세션 기반의 장바구니이기 떄문에 Cart 도메인을 사용하지 않음.
 * 추후 회원별 장바구니를 DB에 영속 저장하고자 할 때 Cart 도메인 사용을 고려할 수도 있기에 남겨놓음.
 */
@Getter
public class CartItem {
    private final Long productId;
    private int quantity;

    protected CartItem(Long productId, int quantity) {
        // 생성자 생성 시점에서 검증 로직 넣기
        if(productId == null) {
            throw new IllegalArgumentException("productId은 필수입니다.");
        }
        if(quantity <= 0) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    public static CartItem of(Long productId, int quantity) {
        return new CartItem(productId, quantity);
    }

    public void addQuantity(int amount) {
        if(amount <= 0) {
            throw new IllegalArgumentException("추가 수량은 1 이상이어야 합니다.");
        }
        this.quantity = quantity + amount;
    }

    public void changeQuantity(int quantity) {
        if(quantity <= 0) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
