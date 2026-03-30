package io.github.takgeun.shop.product.domain;

import io.github.takgeun.shop.global.error.ConflictException;
import lombok.Data;
import lombok.Getter;

@Getter
public class Product {
    private Long id;
    private Long categoryId;

    private String name;
    private int price;      // 판매가
    private int stock;
    private String description;
    private ProductStatus status;

    private String imageUrl;
    private double rating;          // double 이니 기본값 0.0 (추후 리뷰 구현 시 값 넣기)
    private Integer originalPrice;

    protected Product() {
    }

    private Product(Long categoryId,
                   String name,
                   int price,
                   int stock,
                   String description,
                   ProductStatus status,
                   Integer originalPrice,
                   String imageUrl) {

        // 생성자 생성 시점에서 검증 로직을 넣기
        changeCategory(categoryId);
        changeName(name);
        changePrice(price);
        changeStockOnly(stock);
        changeDescription(description);
        changeImageUrl(imageUrl);
        this.rating = 0.0;
        this.status = ProductStatus.READY;

        changeOriginalPrice(originalPrice);
        applyInitialStatus(status);
        adjustStatusByStock();      // 마지막에 재고를 기준으로 상태 반영
    }

    public static Product create(Long categoryId,
                                 String name,
                                 int price,
                                 int stock,
                                 String description,
                                 ProductStatus status,
                                 Integer originalPrice,
                                 String imageUrl) {
        return new Product(
                categoryId,
                name,
                price,
                stock,
                description,
                status,
                originalPrice,
                imageUrl
        );
    }

    // 상품 생성 시 id가 필요한데, 엔티티에서는 setter 방식으로 id를 만들 수 없으니
    // 우선 assignId 메서드를 직접 만들고 임시로 사용할 것.
    // 추후 JPA를 통해 해결할 예정
    public void assignId(Long id) {
        if(id == null || id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }
        if(this.id != null) {
            throw new ConflictException("id는 이미 할당되었습니다.");
        }
        this.id = id;
    }

    public void changeCategory(Long categoryId) {
        if(categoryId == null) {
            throw new IllegalArgumentException("categoryId는 필수입니다.");
        }
        if(categoryId <= 0) {
            throw new IllegalArgumentException("categoryId는 양수여야 합니다.");
        }
        this.categoryId = categoryId;
    }

    public void changeName(String name) {
        String normalized = normalizeRequiredText(name, "상품명은 필수입니다.");

        if(normalized.length() > 100) {
            throw new IllegalArgumentException("상품명은 100자 이하입니다.");
        }
        this.name = normalized;
    }

    public void changePrice(int price) {
        if(price < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }
        this.price = price;

        // 정가가 판매가보다 작아지는 경우 자동 정리
        // 관리자가 price를 바꾸면 originalPrice보다 price가 더 높은 상황이 생길 수 있음. -> 규칙 위반
        // 판매가 인상으로 정가보다 높아지면 할인 의미가 없어지므로 정가 자동 제거
        if(this.originalPrice != null && this.originalPrice <= this.price) {
            this.originalPrice = null;
        }
    }

    public void changeStock(int stock) {
        changeStockOnly(stock);
        adjustStatusByStock();  // 최종 상태를 재고 기준으로 정리
    }



    public void changeDescription(String description) {
        String normalized = normalizeOptionalText(description);

        if(normalized != null && normalized.length() > 2000) {
            throw new IllegalArgumentException("상품 설명은 2000자 이하여야 합니다.");
        }
        this.description = normalized;
    }

    public void changeOriginalPrice(Integer originalPrice) {
        if(originalPrice == null || originalPrice <= 0) {
            this.originalPrice = null;
            return;
        }
        if(originalPrice < this.price) {
            throw new IllegalArgumentException("정가는 판매가 이상이어야 합니다.");
        }
        if(originalPrice == this.price) {
            this.originalPrice = null;  // 할인 없음
            return;
        }
        this.originalPrice = originalPrice;
    }

    public void changeImageUrl(String imageUrl) {
        String normalized = normalizeOptionalText(imageUrl);

        if(normalized != null && normalized.length() > 500) {
            throw new IllegalArgumentException("imageUrl은 500자 이하여야 합니다.");
        }
        this.imageUrl = normalized;
    }

    public boolean isPublicVisible() {
        return this.status == ProductStatus.ON_SALE || this.status == ProductStatus.SOLD_OUT;
    }

    public boolean isOnSale() {
        return this.status == ProductStatus.ON_SALE;
    }

    public boolean isSoldOut() {
        return this.status == ProductStatus.SOLD_OUT;
    }

    public boolean isHidden() {
        return this.status == ProductStatus.HIDDEN;
    }

    public boolean isReady() {
        return this.status == ProductStatus.READY;
    }

    public boolean isDiscontinued() {
        return this.status == ProductStatus.DISCONTINUED;
    }

    public void ready() {
        if (isDiscontinued()) {
            throw new ConflictException("판매 종료된 상품은 READY 상태로 변경할 수 없습니다.");
        }
        if (isReady()) {
            return;
        }
        this.status = ProductStatus.READY;
    }

    public void onSale() {
        if (isDiscontinued()) {
            throw new ConflictException("판매 종료된 상품은 판매중으로 변경할 수 없습니다.");
        }
        if (this.stock == 0) {
            throw new ConflictException("재고가 0인 상품은 판매중으로 변경할 수 없습니다.");
        }
        if (isOnSale()) {
            return;
        }
        this.status = ProductStatus.ON_SALE;
    }

    public void hide() {
        if (isDiscontinued()) {
            throw new ConflictException("판매 종료된 상품은 숨김으로 변경할 수 없습니다.");
        }
        if (isHidden()) {
            return;
        }
        this.status = ProductStatus.HIDDEN;
    }

    public void discontinue() {
        if(this.status == ProductStatus.DISCONTINUED) return;
        this.status = ProductStatus.DISCONTINUED;
    }

    public void decreaseStock(int quantity) {

        if(quantity <= 0) {
            throw new IllegalArgumentException("감소 수량은 1 이상이어야 합니다.");
        }

        if(this.stock < quantity) {
            throw new ConflictException("주문 수량이 판매 중인 상품의 재고보다 많습니다. 현재 재고 : " + this.stock);
        }
        this.stock = this.stock - quantity;

        adjustStatusByStock();
    }

    public void increaseStock(int quantity) {
        if(quantity <= 0) {
            throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다.");
        }
        this.stock = this.stock + quantity;

        adjustStatusByStock();
    }

    // 할인율 계산
    public int discountPercent() {
        if (originalPrice == null || originalPrice <= 0 || originalPrice <= price) {
            return 0;
        }
        return (int) Math.round((1 - (double) price / originalPrice) * 100);
    }

    // 레이팅 (추후 구현)
    public double getRatingValue() {
        return rating;
    }

    private void applyInitialStatus(ProductStatus status) {
        ProductStatus target = (status == null) ? ProductStatus.READY : status;

        switch (target) {
            case READY -> ready();
            case ON_SALE -> onSale();
            case HIDDEN -> hide();
            case SOLD_OUT -> {
                if(this.stock > 0) {
                    throw new IllegalArgumentException("재고가 있는 상품은 초기 상태를 SOLD_OUT으로 설정할 수 없습니다.");
                }
                this.status = ProductStatus.SOLD_OUT;
            }
            case DISCONTINUED -> discontinue();
        }
    }

    // 재고에 따른 상태 변경 (SOLD_OUT vs ON_SALE)
    private void adjustStatusByStock() {
        if(this.status != ProductStatus.ON_SALE && this.status != ProductStatus.SOLD_OUT) {
            return;
        }
        this.status = (this.stock == 0) ? ProductStatus.SOLD_OUT : ProductStatus.ON_SALE;
    }

    private String normalizeRequiredText(String value, String message) {
        if(value == null) {
            throw new IllegalArgumentException(message);
        }

        String normalized = value.trim();
        if(normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if(value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void changeStockOnly(int stock) {
        if(stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
        this.stock = stock;
    }
}
