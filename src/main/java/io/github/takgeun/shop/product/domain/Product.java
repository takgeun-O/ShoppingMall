package io.github.takgeun.shop.product.domain;

// Domain(Entity/Model)

import io.github.takgeun.shop.global.error.ConflictException;
import lombok.Getter;

@Getter
public class Product {
    private Long id;
    private Long categoryId;

    private String name;
    private int price;
    private int stock;
    private String description;
    private ProductStatus status;

    protected Product() {
    }

    private Product(Long categoryId, String name, int price, int stock, String description) {
        // 생성자 생성 시점에서 검증 로직을 넣기
        changeCategory(categoryId);
        changeName(name);
        changePrice(price);
        changeStock(stock);
        changeDescription(description);
        this.status = ProductStatus.ON_SALE;
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

    public static Product create(Long categoryId, String name, int price, int stock, String description) {
        // static 을 사용하는 이유
        // 1. 이름 검증은 생성자/도메인 메서드에서 반드시 수행되도록 하기 위함
        // 2. 생성 시점의 도메인 규칙을 한 곳에 고정시키게 하기 위함.
        // 비즈니스 의미가 있는 객체는 거의 다 static factory가 더 좋다.
        return new Product(categoryId, name, price, stock, description);
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
        if(name == null) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        String normalized = name.trim();
        if(normalized.isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
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
    }

    public void changeStock(int stock) {
        if(stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
        this.stock = stock;

        if(this.stock == 0 && this.status == ProductStatus.ON_SALE) {
            this.status = ProductStatus.SOLD_OUT;
        }
        if(this.stock > 0 && this.status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public void changeDescription(String description) {
        if(description == null) {
            this.description = null;
            return;
        }

        String normalized = description.trim();
        if(normalized.isEmpty()) {
            this.description = null;
            return;
        }
        if(normalized.length() > 2000) {
            throw new IllegalArgumentException("상품 설명은 2000자 이하여야 합니다.");
        }
        this.description = normalized;
    }

    public boolean isPublicVisible() {
        if(this.status == ProductStatus.HIDDEN || this.status == ProductStatus.DISCONTINUED) {
            return false;
        }
        return true;
    }

    public void onSale() {
        if(this.status == ProductStatus.DISCONTINUED) {
            throw new ConflictException("판매 종료된 상품은 판매중으로 변경할 수 없습니다.");
        }
        if(this.stock == 0) {
            throw new ConflictException("재고가 0인 상품은 판매중으로 변경할 수 없습니다.");
        }
        if(this.status == ProductStatus.ON_SALE) return;        // 멱등 처리
        this.status = ProductStatus.ON_SALE;
    }

    public void hide() {
        if(this.status == ProductStatus.DISCONTINUED) {
            throw new ConflictException("판매 종료된 상품은 숨김으로 변경할 수 없습니다.");
        }
        if(this.status == ProductStatus.HIDDEN) {
            return;     // 멱등 처리
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
    }

    public void increaseStock(int quantity) {
        if(quantity <= 0) {
            throw new IllegalArgumentException("증가 수량은 1 이상이어야 합니다.");
        }
        this.stock = this.stock + quantity;

        // 재고가 0 -> 양수로 바뀌면 자동 ON_SALE 전환
        if(this.stock > 0 && this.status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ON_SALE;
        }
    }
}
