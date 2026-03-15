package io.github.takgeun.shop.order.view.form;

import io.github.takgeun.shop.global.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutForm {

    /**
     * 중복 주문 방지용 요청 키
     */
    @NotBlank(message = "잘못된 주문 요청입니다.", groups = ValidationGroups.Required.class)
    @Size(max = 100, message = "잘못된 주문 요청입니다.")
    private String requestKey;

    /**
     * 받는 분 성함
     * 연락처
     * 우편번호
     * 주소
     * 상세주소
     * 요청사항
     * (히든 입력) requestKey
     */

    @NotBlank(message = "받는 분 성함을 입력해주세요.", groups = ValidationGroups.Required.class)
    @Size(max = 30, message = "받는 분 성함은 30자 이하여야 합니다.")
    private String recipientName;

    @NotBlank(message = "연락처를 입력해주세요.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.", groups = ValidationGroups.Format.class)
    private String phoneNumber;

    @NotBlank(message = "우편번호를 입력해주세요.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = "^[0-9]{5}$", message = "우편번호는 5자리 숫자여야 합니다.", groups = ValidationGroups.Format.class)
    private String zipCode;

    @NotBlank(message = "주소를 입력해주세요.", groups = ValidationGroups.Required.class)
    @Size(max = 100, message = "주소는 100자 이하여야 합니다.")
    private String address;

    @NotBlank(message = "상세주소를 입력해주세요.", groups = ValidationGroups.Required.class)
    @Size(max = 100, message = "상세주소는 100자 이하여야 합니다.")
    private String addressDetail;

    @Size(max = 200, message = "requestMessage은 200자 이하입니다.")
    private String requestMessage;
}
