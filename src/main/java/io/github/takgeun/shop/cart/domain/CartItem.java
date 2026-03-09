package io.github.takgeun.shop.cart.domain;

import lombok.Getter;

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
