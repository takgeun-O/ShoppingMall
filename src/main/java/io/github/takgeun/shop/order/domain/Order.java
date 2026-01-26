package io.github.takgeun.shop.order.domain;

import io.github.takgeun.shop.global.error.ConflictException;
import lombok.Getter;

import java.time.LocalDateTime;

// 도메인은 불변 규칙과 상태변이 적기
@Getter
public class Order {
    private Long id;
    private Long memberId;
    private OrderStatus status;

    private Long productId;
    private String productNameSnapshot;
    private int unitPriceSnapshot;
    private int quantity;
    private int totalPrice;

    // 배송 정보
    private String recipientName;
    private String recipientPhone;
    private String shippingZipCode;
    private String shippingAddress;
    private String requestMessage;

    private LocalDateTime orderedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime updatedAt;

    protected Order() {
    }

    private Order(
            Long memberId,
            Long productId,
            String productNameSnapshot,
            int unitPriceSnapshot,
            int quantity,
            String recipientName,
            String recipientPhone,
            String shippingZipCode,
            String shippingAddress,
            String requestMessage
    ) {
        // 생성자 생성 시점에서 검증 로직 넣기
        validateCreate(memberId, productId, productNameSnapshot, unitPriceSnapshot, quantity, recipientName, recipientPhone,
                shippingZipCode, shippingAddress, requestMessage);

        this.memberId = memberId;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
        this.totalPrice = unitPriceSnapshot * quantity;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingZipCode = shippingZipCode;
        this.shippingAddress = shippingAddress;
        this.requestMessage = requestMessage;

        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
        this.updatedAt = this.orderedAt;
    }

    public void assignId(Long id) {
        if(id == null || id <= 0) throw new IllegalArgumentException("id는 양수여야 합니다.");
        if(this.id != null) throw new ConflictException("id는 이미 할당되어 있습니다.");

        this.id = id;
    }

    public static Order create(Long memberId, Long productId, String productNameSnapshot,
                               int unitPriceSnapshot, int quantity,
                               String recipientName, String recipientPhone,
                               String shippingZipCode, String shippingAddress, String requestMessage) {
        return new Order(memberId, productId, productNameSnapshot, unitPriceSnapshot, quantity, recipientName, recipientPhone, shippingZipCode, shippingAddress, requestMessage);
    }

    public void cancel() {
        if (this.status != OrderStatus.ORDERED) {
            throw new IllegalArgumentException("ORDERED 상태에서만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.updatedAt = this.canceledAt;
    }

    private static void validateCreate(Long memberId, Long productId, String productNameSnapshot,
                                       int unitPriceSnapshot, int quantity,
                                       String recipientName, String recipientPhone,
                                       String shippingZipCode, String shippingAddress, String requestMessage) {

        if(memberId == null) throw new IllegalArgumentException("memberId는 필수입니다.");
        if(productId == null) throw new IllegalArgumentException("productId는 필수입니다.");
        requireText(productNameSnapshot, "productNameSnapshot은 필수입니다.");
        if(unitPriceSnapshot < 0) throw new IllegalArgumentException("unitPriceSnapshot은 0 이상입니다.");
        if(quantity < 1) throw new IllegalArgumentException("quantity는 1 이상입니다.");

        requireText(recipientName, "recipientName은 필수입니다.");
        if(recipientName.trim().length() > 50) throw new IllegalArgumentException("recipientName은 50자 이하입니다.");

        requireText(recipientPhone, "recipientPhone 필수입니다.");
        if(!recipientPhone.trim().matches("^[0-9\\-]{9,20}$")) throw new IllegalArgumentException("recipientPhone 형식이 올바르지 않습니다.");

        requireText(shippingZipCode, "shippingZipCode 필수입니다.");
        if(shippingZipCode.trim().length() > 10) throw new IllegalArgumentException("shippingZipCode는 10자 이하입니다.");

        requireText(shippingAddress, "shippingAddress 필수입니다.");
        if(shippingAddress.trim().length() > 200) throw new IllegalArgumentException("shippingAddress는 200자 이하입니다.");

        if(requestMessage != null && requestMessage.trim().length() > 200) throw new IllegalArgumentException("requestMessage는 200자 이하입니다.");
    }

    private static void requireText(String value, String message) {
        if(value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
    }
}
