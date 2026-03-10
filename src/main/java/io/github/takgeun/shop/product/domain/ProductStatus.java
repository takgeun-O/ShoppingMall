package io.github.takgeun.shop.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    READY("판매 준비 중", "상품이 아직 판매 준비 중이며 고객이 구매할 수 없는 상태입니다."),
    ON_SALE("판매 중", "상품이 고객에게 노출되며 바로 구매 가능한 상태입니다."),
    HIDDEN("숨김 처리", "상품은 저장되어 있지만 고객 화면에는 노출되지 않습니다."),
    SOLD_OUT("품절", "상품이 일시적으로 품절되어 고객이 구매할 수 없습니다."),
    DISCONTINUED("판매 종료", "상품 판매가 종료된 상태로 더 이상 판매하지 않습니다.");

    private final String label;
    private final String description;

    public String getDisplayName() {
        return name() + " - " + label;
    }
}
