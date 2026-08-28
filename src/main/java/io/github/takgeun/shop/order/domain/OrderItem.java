package io.github.takgeun.shop.order.domain;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import lombok.Getter;

@Getter
//@NoArgsConstructor        // 이거 쓰면 public 기본생성자가 만들어지는데 바깥에서 생성 못하도록 하기 위해 사용 X
public class OrderItem {

    private static final String DEFAULT_IMAGE_URL = "/images/no-image.png";
    private static final int MAX_PRODUCT_NAME_LENGTH = 200;
    private static final int MAX_IMAGE_URL_LENGTH = 500;

    private Long id;
    private Long orderId;
    private Long productId;
    private String productNameSnapshot;
    private int unitPriceSnapshot;
    private Integer originalPriceSnapshot;  // 정가 (nullable 가능)
    private int quantity;
    private String imageUrlSnapshot;

    protected OrderItem() {}

    /**
     * 생성 시점 불변식 검증을 도메인이 직접 책임
     * 이유 : 도메인 객체는 잘못된 상태로 만들어지면 안됨.
     */
    private OrderItem(Long productId,
                      String productNameSnapshot,
                      int unitPriceSnapshot,
                      Integer originalPriceSnapshot,
                      int quantity,
                      String imageUrlSnapshot)
    {
        validateProductId(productId);
        validateProductName(productNameSnapshot);
        validateUnitPrice(unitPriceSnapshot);
        validateOriginalPrice(originalPriceSnapshot, unitPriceSnapshot);
        validateQuantity(quantity);

        String normalizedProductName = productNameSnapshot.trim();
        String resolvedImageUrl = resolveImageUrl(imageUrlSnapshot);

        this.productId = productId;
        this.productNameSnapshot = normalizedProductName;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.originalPriceSnapshot = originalPriceSnapshot;
        this.quantity = quantity;
        this.imageUrlSnapshot = resolvedImageUrl;
    }

    public static OrderItem of(Long productId,
                               String productNameSnapshot,
                               int unitPriceSnapshot,
                               Integer originalPriceSnapshot,
                               int quantity,
                               String imageUrlSnapshot) {
        return new OrderItem(
                productId,
                productNameSnapshot,
                unitPriceSnapshot,
                originalPriceSnapshot,
                quantity,
                imageUrlSnapshot
        );
    }

    public void assignId(Long id) {
        if(id == null || id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }
        if(this.id != null) {
            throw new ConflictException("id는 이미 할당되어 있습니다.");
        }
        this.id = id;
    }

    public void assignOrderId(Long orderId) {
        if(orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId는 양수여야 합니다.");
        }
        if(this.orderId != null) {
            throw new ConflictException("orderId는 이미 할당되어 있습니다.");
        }
        this.orderId = orderId;
    }

    public int lineTotal() {
        return unitPriceSnapshot * quantity;
    }

    public int originalLineTotal() {
        if (originalPriceSnapshot == null) {
            return 0;
        }
        return originalPriceSnapshot * quantity;
    }

    public int discountAmount() {
        if (originalPriceSnapshot == null) {
            return 0;
        }

        int unitDiscount = originalPriceSnapshot - unitPriceSnapshot;
        if (unitDiscount <= 0) {
            return 0;
        }

        return unitDiscount * quantity;
    }

    public Integer discountRatePercent() {
        if (originalPriceSnapshot == null) {
            return null;
        }
        if (originalPriceSnapshot <= unitPriceSnapshot) {
            return null;
        }

        double rate = ((double) (originalPriceSnapshot - unitPriceSnapshot)
                / (double) originalPriceSnapshot) * 100.0;

        return (int) Math.round(rate);
    }




    private void validateProductId(Long productId) {
        if(productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId는 양수여야 합니다.");
        }
    }

    private void validateProductName(String productNameSnapshot) {
        requireText(productNameSnapshot, "productNameSnapshot은 필수입니다.");
        if(productNameSnapshot.trim().length() > MAX_PRODUCT_NAME_LENGTH) {
            throw new IllegalArgumentException("productNameSnapshot은 최대 " + MAX_PRODUCT_NAME_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateUnitPrice(int unitPriceSnapshot) {
        if(unitPriceSnapshot < 0) {
            throw new IllegalArgumentException("unitPriceSnapshot은 0 이상이어야 합니다.");
        }
    }

    private void validateOriginalPrice(Integer originalPriceSnapshot, int unitPriceSnapshot) {
        if(originalPriceSnapshot == null) {
            return;
        }

        // 정가는 0 초과해야 함.
        if(originalPriceSnapshot <= 0) {
            throw new IllegalArgumentException("originalPriceSnapshot은 0 초과여야 합니다.");
        }

        // 정가는 판매가 이상
        if(originalPriceSnapshot < unitPriceSnapshot) {
            throw new IllegalArgumentException("정가는 판매가 이상이어야 합니다.");
        }
    }

    private void validateQuantity(int quantity) {
        if(quantity < 1) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
    }

    private String resolveImageUrl(String imageUrlSnapshot) {
        String resolved = (imageUrlSnapshot == null || imageUrlSnapshot.isBlank())
                ? DEFAULT_IMAGE_URL
                : imageUrlSnapshot.trim();

        if(resolved.length() > MAX_IMAGE_URL_LENGTH) {
            throw new IllegalArgumentException("imageUrlSnapshot은 최대 " + MAX_IMAGE_URL_LENGTH + "자까지 가능합니다.");
        }

        return resolved;
    }

    private void requireText(String value, String message) {
        if(value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
