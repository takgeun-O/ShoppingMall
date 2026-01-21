package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductStatusUpdateRequest {

    @NotNull
    private ProductStatus status;
}
