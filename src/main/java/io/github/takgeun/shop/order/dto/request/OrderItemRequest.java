package io.github.takgeun.shop.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor             // 기본 생성자는 필수. Jackson이 기본 생성자로 객체 생성 후 필드에 값 주입하니까.
//@AllArgsConstructor         // 요청 DTO는 프레임워크가 만드는 객체임. 스프링이 JSON을 객체로 자동 바인딩할 것이므로 필요 없음.
public class OrderItemRequest {
    private Long productId;
    private int quantity;
}
