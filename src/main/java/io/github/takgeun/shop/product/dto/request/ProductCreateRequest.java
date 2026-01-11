package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor             // 기본 생성자는 필수. Jackson이 기본 생성자로 객체 생성 후 필드에 값 주입하니까.
//@AllArgsConstructor         // 요청 DTO는 프레임워크가 만드는 객체임. 스프링이 JSON을 객체로 자동 바인딩할 것이므로 필요 없음.
public class ProductCreateRequest {
    private Long categoryId;
    private String name;
    private int price;
    private int stock;
    private String description;
    //    private boolean active;       // 카테고리 생성 시 active는 서버에서 기본값 true로 고정. (보통 등록할 때 활성화시키는 게 자연스러움)
}