package io.github.takgeun.shop.category.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
//@AllArgsConstructor         // 요청 DTO는 프레임워크가 만드는 객체임. 스프링이 JSON을 객체로 자동 바인딩할 것이므로 필요 없음.
@NoArgsConstructor             // 기본 생성자는 필수. Jackson이 기본 생성자로 객체 생성 후 필드에 값 주입하니까.
public class CategoryCreateRequest {
    private String name;
    private Long parentId;      // 최상위일 경우 null
//    private boolean active;       // 카테고리 생성 시 active는 서버에서 기본값 true로 고정. (보통 등록할 때 활성화시키는 게 자연스러움)
}
