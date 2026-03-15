package io.github.takgeun.shop.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateOrderCommand {
    // 주문 생성용 Command DTO (서비스로 넘길 요청)
    // 화면에 뿌리기 위한 DTO가 아니라 서비스가 주문을 생성하기 위해 필요로 하는 입력 데이터 묶음
    private String recipientName;
    private String phoneNumber;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String requestMessage;

    // 중복 주문 방지용
    private String requestKey;
}
