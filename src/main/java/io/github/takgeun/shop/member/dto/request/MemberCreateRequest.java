package io.github.takgeun.shop.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

//@Data         // @Data의 역할이 과도함. (setter를 여기서 쓰지 않는데 DTO는 보통 불변으로 쓰여서 setter를 쓰지 않음.) 즉 @Data는 가변 객체를 전제로 쓰인다.
@Getter
@AllArgsConstructor
// 이 부분은 class 대신 record 사용해서 추후 교체 예정
public class MemberCreateRequest {
    private final String email;
    private final String password;
    private final String name;
    private final String phoneNumber;
}
