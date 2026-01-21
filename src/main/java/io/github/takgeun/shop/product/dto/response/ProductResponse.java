package io.github.takgeun.shop.product.dto.response;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private Long categoryId;
    private String name;
    private int price;
    private int stock;
    private String description;
    private ProductStatus status;

    // 도메인 객체(Product)를 응답 DTO(ProductResponse)로 변환할 때 쓰는 전용 메서드
    // 컨트롤러나 서비스 등 다른 곳에서 아래 코드가 반복되는 걸 방지하기 위함.
    // 또는 컨트롤러나 서비스가 DTO 구조를 몰라도 된다는 장점도 있음. (변경에 강함)
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getStatus()
        );
    }
}
