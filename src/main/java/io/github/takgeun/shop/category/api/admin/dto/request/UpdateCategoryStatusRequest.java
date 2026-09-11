package io.github.takgeun.shop.category.api.admin.dto.request;

import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 카테고리의 이름, 부모 변경과 활성 상태 변경은 의미가 다름
 *
 * UpdateCategoryRequest에 status까지 몰아넣으면 발생할 수 있는 문제
 * 1. 요청 의도가 모호해짐
 * {
 *   "name": null,
 *   "parentId": null,
 *   "status": "INACTIVE"
 * }
 * --> null이 의미하는 바는 무엇인가?
 *      이름을 null로 변경?
 *      이름을 변경하지 않음?
 *      잘못된 요청?
 * 특히 parentId는 최상위 카테고리를 의미하기 위해 실제 값으로 null을 사용할 수 있기까지 함...
 * 2. 검증 조건이 복잡해짐
 * 정보 수정에서 name이 필수이지만, 상태 변경 요청에서는 name이 필수가 아님
 * 상태만 변경하고 싶을 때 이름과 부모까지 보내야 함.
 * 그렇다고 DTO에서 검증 애노테이션 없이 모두 선택값으로 만들면 형식상 통과할 수는 있으나
 * 결국 Controller나 Service에서 조건문이 늘어남.
 * 3. 서로 다른 권한이나 감사 정책을 적용하기 어려움
 * 현재는 모두 관리자 권한으로서 이름 변경과 상태 변경을 할 수 있으나,
 * 향후 정책이 달라져 이름 변경은 일반 상품 관리자만 가능하고 카테고리 비활성화는 상위 관리자만 가능할 경우
 * 엔드포인트와 서비스가 분리되어 있으면 적용하기 쉽다.
 */
@Schema(description = "관리자 카테고리 상태 변경 요청")
public record UpdateCategoryStatusRequest(

        @Schema(
                description = "변경할 카테고리 선택",
                example = "INACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "카테고리 상태는 필수입니다.")
        CategoryStatus status
) {
}
