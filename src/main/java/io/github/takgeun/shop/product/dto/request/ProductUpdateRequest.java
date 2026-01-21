package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {     // 부분 수정

    @Positive(message = "categoryId는 양수여야 합니다.")
    private Long categoryId;            // null이면 변경 없음

    @Size(max = 100, message = "상품명은 100자 이하입니다.")
    @NotBlank(message = "상품명은 비어 있을 수 없습니다.")       // NotBlank는 null 일 때는 검증 통과함
    private String name;                // null이면 변경 없음. ""면 검증 실패

    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;              // null 이면 변경 안함

    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stock;              // null 이면 변경 안함

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;         // null 이면 변경 안함 (설명 삭제는 빈 문자열로)

    public static ProductUpdateRequest of(
            Long categoryId,
            String name,
            Integer price,
            Integer stock,
            String description
    ) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.categoryId = categoryId;
        request.name = name;
        request.price = price;
        request.stock = stock;
        request.description = description;
        return request;
    }
}
