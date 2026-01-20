package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {     // 부분 수정

    private Long categoryId;            // 변경 시에만

    @Size(max = 100, message = "상품명은 100자 이하입니다.")
    private String name;                // 변경 시에만

    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;              // null 이면 변경 안함

    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stock;              // null 이면 변경 안함

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;         // null 이면 변경 안함 (설명 삭제는 빈 문자열로)


    private Boolean active;             // null 이면 변경 안함
    // wrapper 타입 : 수정 요청에서 active를 보내지 않았을 경우 null로 처리하기 위함 (null로 변경 없음 표현 가능)
}
