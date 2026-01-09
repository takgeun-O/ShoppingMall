package io.github.takgeun.shop.category.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CategoryUpdateRequest {
    private String name;        // null 이면 변경 안함
    private Long parentId;      // null 로 요청 받으면 최상위로 변경, 전달 받지 않으면 변경 없음
    private Boolean active;         // wrapper 타입 : 수정 요청에서 active를 보내지 않았을 경우 null로 처리하기 위함 (null로 변경 없음 표현 가능)
}
