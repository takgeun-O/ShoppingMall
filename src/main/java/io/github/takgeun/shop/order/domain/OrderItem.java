package io.github.takgeun.shop.order.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
//@NoArgsConstructor        // 이거 쓰면 public 기본생성자가 만들어지는데 바깥에서 생성 못하도록 하기 위해 사용 X
public class OrderItem {

    /**
     * Order : 여러 상품을 담는 Aggregate Root
     * OrderItem : 각 내부 구성요소
     */

    private Long productId;
    private String productNameSnapshot;
    private int unitPriceSnapshot;
    private Integer originalPriceSnapshot;  // 정가 (nullable 가능)
    private int quantity;
    private String imageUrlSnapshot;

    protected OrderItem() {}

    private OrderItem(Long productId, String name, int unitPrice, Integer originalPrice, int quantity, String imageUrl) {
        if(productId == null) throw new IllegalArgumentException("productId는 필수입니다.");
        if(name == null || name.trim().isBlank()) throw new IllegalArgumentException("productNameSnapshot은 필수입니다.");
        if(unitPrice < 0) throw new IllegalArgumentException("unitPriceSnapshot은 0 이상입니다.");
        if(originalPrice != null) {
            if(originalPrice <= 0) {
                throw new IllegalArgumentException("originalPriceSnapshot은 0 초과여야 합니다.");
            }
            if(originalPrice < unitPrice) {
                throw new IllegalArgumentException("정가는 판매가 이상이어야 합니다.");
            }
        }
        if(quantity < 1) throw new IllegalArgumentException("quantity는 1 이상입니다.");
        if(imageUrl == null || imageUrl.isBlank()) {
            imageUrl = "/images/no-image.png";
        }

        this.productId = productId;
        this.productNameSnapshot = name;
        this.unitPriceSnapshot = unitPrice;
        this.originalPriceSnapshot = originalPrice;
        this.quantity = quantity;
        this.imageUrlSnapshot = (imageUrl == null || imageUrl.isBlank())
                ? "/images/no-image.png"
                : imageUrl;
    }

    // of() : 값을 모아서 만든다.
    // Money of(int amount, String currency) return new Money(amount, currency);
    // from() : 어떤 객체를 기반으로 만들어진다.
    // ProductCardView from(Product p) : Product -> ProductCardView 변환
    public static OrderItem of(Long productId, String name, int unitPrice, Integer originalPrice, int quantity, String imageUrl) {
        return new OrderItem(productId, name, unitPrice, originalPrice, quantity, imageUrl);
    }

    public int lineTotal() {
        return unitPriceSnapshot * quantity;
    }

    public int originalLineTotal() {
        if (originalPriceSnapshot == null) return 0;
        return originalPriceSnapshot * quantity;
    }

    public int discountAmount() {
        if (originalPriceSnapshot == null) return 0;

        int unitDiscount = originalPriceSnapshot - unitPriceSnapshot;
        if (unitDiscount <= 0) return 0;

        return unitDiscount * quantity;
    }

    public Integer discountRatePercent() {
        if (originalPriceSnapshot == null) return null;
        if (originalPriceSnapshot <= unitPriceSnapshot) return null;

        double rate = ((double) (originalPriceSnapshot - unitPriceSnapshot)
                / (double) originalPriceSnapshot) * 100.0;

        return (int) Math.round(rate);
    }

    public boolean hasDiscount() {
        return discountRatePercent() != null;
    }
}
