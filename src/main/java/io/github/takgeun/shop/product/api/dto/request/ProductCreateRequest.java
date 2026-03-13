package io.github.takgeun.shop.product.api.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter                         // 요청DTO에서 @Setter 대부분 필요 (스프링이 객체를 바인딩하는 방식)
@NoArgsConstructor             // 기본 생성자는 필수. Jackson이 기본 생성자로 객체 생성 후 필드에 값 주입하니까.
//@AllArgsConstructor         // 요청 DTO는 프레임워크가 만드는 객체임. 스프링이 JSON을 객체로 자동 바인딩할 것이므로 필요 없음.
public class ProductCreateRequest {
    @NotNull(message = "categoryId는 필수입니다.")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하입니다.")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;

    @Min(value = 1, message = "정가는 1 이상이어야 합니다.")
    private Integer originalPrice;

    @NotNull(message = "재고는 필수입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stock;

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;

    @Size(max = 500, message = "imageUrl은 500자 이하입니다.")
    private String imageUrl;

    @NotNull(message = "status는 필수입니다.")
    private ProductStatus status;
}