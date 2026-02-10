package io.github.takgeun.shop.order.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor          // 기본 생성자는 필수. Jackson이 기본 생성자로 객체 생성 후 필드에 값 주입하니까.
//@AllArgsConstructor       // 요청 DTO는 프레임워크가 만드는 객체임. 스프링이 JSON을 객체로 자동 바인딩할 것이므로 필요 없음.
public class OrderCreateRequest {

    @NotNull(message = "productId는 필수입니다.")
    private Long productId;

    @NotNull(message = "quantity는 필수입니다.")
    @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
    private Integer quantity;

    @NotBlank(message = "recipientName은 필수입니다.")
    @Size(max = 50, message = "recipientName은 50자 이하입니다.")
    private String recipientName;

    @NotBlank(message = "recipientPhone은 필수입니다.")
    @Pattern(regexp = "^$|^[0-9\\-]{9,20}$", message = "recipientPhone 형식이 올바르지 않습니다.")
    private String recipientPhone;

    @NotBlank(message = "shippingZipCode은 필수입니다.")
    @Size(max = 10, message = "shippingZipCode은 10자 이하입니다.")
    private String shippingZipCode;

    @NotBlank(message = "shippingAddress은 필수입니다.")
    @Size(max = 200, message = "shippingAddress은 200자 이하입니다.")
    private String shippingAddress;

    @Size(max = 200, message = "requestMessage은 200자 이하입니다.")
    private String requestMessage;

    // 주문서 진입 시 productId 고정용
    public OrderCreateRequest(Long productId) {
        this.productId = productId;
    }
}
