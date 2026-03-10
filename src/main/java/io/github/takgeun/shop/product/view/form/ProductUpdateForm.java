package io.github.takgeun.shop.product.view.form;

import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateForm {

    @NotNull
    private Long categoryId;

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer price;

    @Positive
    private Integer originalPrice;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    @NotBlank
    private String description;

    @NotNull
    private ProductStatus status;

    @NotNull
    private String imageUrl;

    public static ProductUpdateForm from(Product product) {
        ProductUpdateForm form = new ProductUpdateForm();
        form.setCategoryId(product.getCategoryId());
        form.setName(product.getName());
        form.setPrice(product.getPrice());
        form.setOriginalPrice(product.getOriginalPrice());
        form.setStock(product.getStock());
        form.setDescription(product.getDescription());
        form.setStatus(product.getStatus());
        form.setImageUrl(product.getImageUrl());
        return form;
    }
}
