package io.github.takgeun.shop.order.domain;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Order {

    private static final int MAX_ORDER_NUMBER_LENGTH = 50;
    private static final int MAX_REQUEST_KEY_LENGTH = 100;
    private static final int MAX_RECIPIENT_NAME_LENGTH = 50;
    private static final int MAX_RECIPIENT_PHONE_LENGTH = 30;
    private static final int MAX_SHIPPING_ZIP_CODE_LENGTH = 20;
    private static final int MAX_SHIPPING_ADDRESS_LENGTH = 200;
    private static final int MAX_SHIPPING_ADDRESS_DETAIL_LENGTH = 200;
    private static final int MAX_REQUEST_MESSAGE_LENGTH = 200;

    /**
     * Order : 여러 상품을 담는 Aggregate Root
     * OrderItem : 각 내부 구성요소
     */

    private Long id;                // DB PK
    private String orderNumber;     // 외부 노출용 주문번호
    private Long memberId;
    private OrderStatus status;
    private String requestKey;      // 이미 처리된 요청인지 판별

    private List<OrderItem> orderItems;     // 이건 마이페이지 주문목록 조회 시점에 OrderItem을 별도로 조회해서 Order에 붙이는 방식으로 aggregate를 완성한다....

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
        validateRequestKey(requestKey);
        validateOrderItems(orderItems);
        validateRecipientName(recipientName);
        validateRecipientPhone(recipientPhone);
        validateShippingZipCode(shippingZipCode);
        validateShippingAddress(shippingAddress);
        validateShippingAddressDetail(shippingAddressDetail);
        validateShippingFee(shippingFee);

        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.requestKey = requestKey;
        this.orderItems = List.copyOf(orderItems);

        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingZipCode = shippingZipCode;
        this.shippingAddress = shippingAddress;
        this.shippingAddressDetail = shippingAddressDetail;
        this.requestMessage = normalizeRequestMessage(requestMessage);

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

    public void replaceOrderItems(List<OrderItem> orderItems) {
        if(orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 상품은 1개 이상이어야 합니다.");
        }
        this.orderItems = List.copyOf(orderItems);
    }

    private void validateOrderItems(List<OrderItem> orderItems) {
        if(orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 상품은 1개 이상이어야 합니다.");
        }
    }

    private void validateOrderNumber(String orderNumber) {
        requireText(orderNumber, "orderNumber는 필수입니다.");
        if(orderNumber.length() > MAX_ORDER_NUMBER_LENGTH) {
            throw new IllegalArgumentException("orderNumber는 최대 " + MAX_ORDER_NUMBER_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateRequestKey(String requestKey) {
        requireText(requestKey, "requestKey는 필수입니다.");
        if(requestKey.length() > MAX_REQUEST_KEY_LENGTH) {
            throw new IllegalArgumentException("requestKey는 최대 " + MAX_REQUEST_KEY_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateRecipientName(String recipientName) {
        requireText(recipientName, "recipientName은 필수입니다.");
        if(recipientName.length() > MAX_RECIPIENT_NAME_LENGTH) {
            throw new IllegalArgumentException("recipientName은 최대 " + MAX_RECIPIENT_NAME_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateRecipientPhone(String recipientPhone) {
        requireText(recipientPhone, "recipientPhone은 필수입니다.");
        if(recipientPhone.length() > MAX_RECIPIENT_PHONE_LENGTH) {
            throw new IllegalArgumentException("recipientPhone은 최대 " + MAX_RECIPIENT_PHONE_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateShippingZipCode(String shippingZipCode) {
        requireText(shippingZipCode, "shippingZipCode는 필수입니다.");
        if(shippingZipCode.length() > MAX_SHIPPING_ZIP_CODE_LENGTH) {
            throw new IllegalArgumentException("shippingZipCode는 최대 " + MAX_SHIPPING_ZIP_CODE_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateShippingAddress(String shippingAddress) {
        requireText(shippingAddress, "shippingAddress는 필수입니다.");
        if(shippingAddress.length() > MAX_SHIPPING_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("shippingAddress는 최대 " + MAX_SHIPPING_ADDRESS_LENGTH + "자까지 가능합니다.");
        }
    }

    private void validateShippingAddressDetail(String shippingAddressDetail) {
        requireText(shippingAddressDetail, "shippingAddressDetail은 필수입니다.");
        if(shippingAddressDetail.length() > MAX_SHIPPING_ADDRESS_DETAIL_LENGTH) {
            throw new IllegalArgumentException("shippingAddressDetail은 최대 " + MAX_SHIPPING_ADDRESS_DETAIL_LENGTH + "자까지 가능합니다.");
        }
    }

    private String normalizeRequestMessage(String requestMessage) {
        if(requestMessage == null) {
            return null;
        }

        String normalized = requestMessage.trim();

        if(normalized.isEmpty()) {
            return null;
        }

        if(normalized.length() > MAX_REQUEST_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                    "requestMessage는 최대 "
                            + MAX_REQUEST_MESSAGE_LENGTH
                            + "자까지 가능합니다."
            );
        }

        return normalized;
    }

    private void validateMemberId(Long memberId) {
        if(memberId == null) {
            throw new IllegalArgumentException("memberId는 필수입니다.");
        }
        if(memberId <= 0) {
            throw new IllegalArgumentException("memberId는 양수여야 합니다.");
        }
    }

    private void validateShippingFee(int shippingFee) {
        if(shippingFee < 0) {
            throw new IllegalArgumentException("shippingFee는 0원 이상이어야 합니다.");
        }
    }

    private void requireText(String value, String message) {
        if(value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
