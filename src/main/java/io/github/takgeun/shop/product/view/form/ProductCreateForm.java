package io.github.takgeun.shop.product.view.form;

import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter // form은 @Setter 허용
public class ProductCreateForm {
    @NotNull(message = "카테고리는 필수입니다.")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하입니다.")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 1, message = "가격은 1 이상이어야 합니다.")
    private Integer price;

    @Min(value = 1, message = "정가는 1 이상이어야 합니다.")
    private Integer originalPrice;

    @NotNull(message = "재고는 필수입니다.")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stock;

    @NotBlank(message = "설명은 필수입니다.")
    @Size(max = 1000, message = "설명은 1000자 이하입니다.")
    private String description;

    @Size(max = 500, message = "이미지 URL은 500자 이하입니다.")
    private String imageUrl;

    @NotNull(message = "상품 상태는 필수입니다.")
    private ProductStatus status;

    public boolean isInvalidPriceRelation() {
        if(price == null || originalPrice == null) {
            return false;
        }
        return price > originalPrice;
    }
}
