package io.github.takgeun.shop.order.view.dto.admin;

import io.github.takgeun.shop.order.domain.OrderItem;
import lombok.Getter;

@Getter
public class AdminOrderProductItemView {

    private final Long productId;
    private final String productName;
    private final String imageUrl;
    private final String option;
    private final int quantity;
    private final int unitPrice;
    private final int subtotal;

    private AdminOrderProductItemView(Long productId,
                                     String productName,
                                     String imageUrl,
                                     String option,
                                     int quantity,
                                     int unitPrice,
                                     int subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.option = option;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public static AdminOrderProductItemView from(OrderItem orderItem) {
        return new AdminOrderProductItemView(
                orderItem.getProductId(),
                orderItem.getProductNameSnapshot(),
                orderItem.getImageUrlSnapshot(),
                null,       // 아직 옵션 구현 전
                orderItem.getQuantity(),
                orderItem.getUnitPriceSnapshot(),
                orderItem.lineTotal()
        );
    }
}
