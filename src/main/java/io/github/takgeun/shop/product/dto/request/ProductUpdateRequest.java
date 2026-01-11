package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {     // 부분 수정
    private Long categoryId;            // 변경 시에만
    private String name;                // 변경 시에만
    private Integer price;              // null 이면 변경 안함
    private Integer stock;              // null 이면 변경 안함
    private String description;         // null 이면 변경 안함 (설명 삭제는 빈 문자열로)
    private Boolean active;             // null 이면 변경 안함
    // wrapper 타입 : 수정 요청에서 active를 보내지 않았을 경우 null로 처리하기 위함 (null로 변경 없음 표현 가능)
}
