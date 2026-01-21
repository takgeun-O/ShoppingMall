package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor             // 기본 생성자는 필수. Jackson이 기본 생성자로 객체 생성 후 필드에 값 주입하니까.
//@AllArgsConstructor         // 요청 DTO는 프레임워크가 만드는 객체임. 스프링이 JSON을 객체로 자동 바인딩할 것이므로 필요 없음.
public class ProductCreateRequest {
    @NotNull(message = "categoryId는 필수입니다.")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하입니다.")
    private String name;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private int price;

    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private int stock;

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;
}