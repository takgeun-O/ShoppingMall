package io.github.takgeun.shop.order.domain;

import io.github.takgeun.shop.global.error.ConflictException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Order {

    /**
     * Order : 여러 상품을 담는 Aggregate Root
     * OrderItem : 각 내부 구성요소
     */

    private Long id;                // DB PK
    private String orderNumber;     // 외부 노출용 주문번호
    private Long memberId;
    private OrderStatus status;
    private String requestKey;

    private List<OrderItem> orderItems;

    // 배송정보
    private String recipientName;
    private String recipientPhone;
    private String shippingZipCode;
    private String shippingAddress;
    private String shippingAddressDetail;
    private String requestMessage;

    private int subtotal;
    private int shippingFee;
    private int totalPrice;

    private LocalDateTime orderedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime updatedAt;

    protected Order() {}

    private Order(Long memberId,
                  String orderNumber,
                  String requestKey,
                  List<OrderItem> orderItems,
                  String recipientName,
                  String recipientPhone,
                  String shippingZipCode,
                  String shippingAddress,
                  String shippingAddressDetail,
                  String requestMessage,
                  int shippingFee) {

        validateMemberId(memberId);
        validateOrderNumber(orderNumber);
        validateOrderItems(orderItems);
        requireText(recipientName, "recipientName은 필수입니다.");
        requireText(recipientPhone, "recipientPhone은 필수입니다.");
        requireText(shippingZipCode, "shippingZipCode는 필수입니다.");
        requireText(shippingAddress, "shippingAddress는 필수입니다.");
        requireText(shippingAddressDetail, "shippingAddressDetail은 필수입니다.");
        validateRequestMessage(requestMessage);

        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.requestKey = requestKey;
        this.orderItems = List.copyOf(orderItems);

        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingZipCode = shippingZipCode;
        this.shippingAddress = shippingAddress;
        this.shippingAddressDetail = shippingAddressDetail;
        this.requestMessage = requestMessage;

        this.subtotal = orderItems.stream()
                .mapToInt(OrderItem::lineTotal)
                .sum();
        this.shippingFee = Math.max(shippingFee, 0);
        this.totalPrice = this.subtotal + this.shippingFee;

        this.status = OrderStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
        this.updatedAt = this.orderedAt;
    }

    public static Order create(Long memberId,
                               String orderNumber,
                               String requestKey,
                               List<OrderItem> orderItems,
                               String recipientName,
                               String recipientPhone,
                               String shippingZipCode,
                               String shippingAddress,
                               String shippingAddressDetail,
                               String requestMessage,
                               int shippingFee) {

        return new Order(
                memberId,
                orderNumber,
                requestKey,
                orderItems,
                recipientName,
                recipientPhone,
                shippingZipCode,
                shippingAddress,
                shippingAddressDetail,
                requestMessage,
                shippingFee
        );
    }

    /**
     * ID는 저장소에서 1회만 할당
     */
    public void assignId(Long id) {
        if(id==null || id<=0) throw new IllegalArgumentException("id는 양수여야 합니다.");
        if(this.id != null) throw new ConflictException("id는 이미 할당되어 있습니다.");
        this.id = id;
    }

    public void markPaymentCompleted() {
        if(this.status != OrderStatus.ORDERED) {
            throw new ConflictException("ORDERED 상태에서만 결제완료 처리할 수 있습니다.");
        }
        this.status = OrderStatus.PAYMENT_COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if(this.status == OrderStatus.PAYMENT_COMPLETED || this.status == OrderStatus.ORDERED) {
            this.status = OrderStatus.CANCELED;
            this.canceledAt = LocalDateTime.now();
            this.updatedAt = this.canceledAt;
            return;
        }

        throw new ConflictException("주문완료 및 결제완료 상태에서만 취소할 수 있습니다.");
    }

    /**
     * ORDERED → PAYMENT_COMPLETED 또는 CANCELED
     * PAYMENT_COMPLETED → PREPARING 또는 CANCELED
     * PREPARING → SHIPPING (이 단계부터 취소 막음.)
     * SHIPPING → DELIVERED
     * DELIVERED / CANCELED → 변경 불가(종료 상태)
     */
    public void changeStatus(OrderStatus newStatus) {
        if(newStatus == null) throw new IllegalArgumentException("status는 필수입니다.");
        if(this.status == newStatus) return;   // 멱등

        // 종료된 상태는 변경 불가
        if(this.status == OrderStatus.CANCELED) throw new ConflictException("취소된 주문은 상태를 변경할 수 없습니다.");
        if(this.status == OrderStatus.DELIVERED) throw new ConflictException("배송 완료된 주문은 상태를 변경할 수 없습니다.");

        // 상태 변경 허용
        boolean allowed = switch (this.status) {
            case ORDERED -> (newStatus == OrderStatus.PAYMENT_COMPLETED || newStatus == OrderStatus.CANCELED);  // 주문완료 -> 결제완료, 취소 변경 가능
            case PAYMENT_COMPLETED -> (newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELED);
            case PREPARING -> (newStatus == OrderStatus.SHIPPING);
            case SHIPPING -> (newStatus == OrderStatus.DELIVERED);
            default -> false;
        };

        if(!allowed) {
            throw new ConflictException("허용되지 않은 상태 변경입니다. (" + this.status + " -> " + newStatus + ")");
        }

        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        if(newStatus == OrderStatus.CANCELED) {
            this.canceledAt = this.updatedAt;
        }
    }

    public boolean isCanceled() {
        return this.status == OrderStatus.CANCELED;
    }


    private void requireText(String value, String message) {
        if(value == null || value.trim().isEmpty()) throw new IllegalArgumentException(message);
    }

    private void validateRequestMessage(String requestMessage) {
        if(requestMessage != null && requestMessage.trim().length() > 200) {
            throw new IllegalArgumentException("requestMessage는 200자 이하입니다.");
        }
    }

    private void validateOrderItems(List<OrderItem> orderItems) {
        if(orderItems == null || orderItems.isEmpty()) throw new IllegalArgumentException("주문 상품은 1개 이상이어야 합니다.");
    }

    private void validateOrderNumber(String orderNumber) {
        if(orderNumber == null || orderNumber.isEmpty()) throw new IllegalArgumentException("orderNumber는 필수입니다.");
    }

    private void validateMemberId(Long memberId) {
        if(memberId == null) throw new IllegalArgumentException("memberId는 필수입니다.");
    }
}
