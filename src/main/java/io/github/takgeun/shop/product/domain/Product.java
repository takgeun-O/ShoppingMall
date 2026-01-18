package io.github.takgeun.shop.product.domain;

// Domain(Entity/Model)

import lombok.Getter;

@Getter
public class Product {
    private Long id;
    private Long categoryId;

    private String name;
    private int price;
    private int stock;
    private String description;
    private boolean active;

    protected Product() {
    }

    public Product(Long categoryId, String name, int price, int stock, String description, boolean active) {
        // 생성자 생성 시점에서 검증 로직을 넣고 싶은 건 따로 메소드에 빼놓기.
//        this.categoryId = categoryId;
//        this.name = name;
//        this.price = price;
//        this.stock = stock;
//        this.description = description;
        changeCategory(categoryId);
        changeName(name);
        changePrice(price);
        changeStock(stock);
        changeDescription(description);
        this.active = active;
    }

    // 상품 생성 시 id가 필요한데, 엔티티에서는 setter 방식으로 id를 만들 수 없으니
    // 우선 assignId 메서드를 직접 만들고 임시로 사용할 것.
    // 추후 JPA를 통해 해결할 예정
    public void assignId(Long id) {
        this.id = id;
    }

    public static Product create(Long categoryId, String name, int price, int stock, String description) {
        // static 을 사용하는 이유
        // 1. 이름 검증은 생성자/도메인 메서드에서 반드시 수행되도록 하기 위함
        // 2. 생성 시점의 도메인 규칙을 한 곳에 고정시키게 하기 위함.
        // 비즈니스 의미가 있는 객체는 거의 다 static factory가 더 좋다.
        return new Product(categoryId, name, price, stock, description, true);
    }

    public void changeCategory(Long categoryId) {
        if(categoryId == null) {
            throw new IllegalArgumentException("categoryId는 필수입니다.");
        }
        this.categoryId = categoryId;
    }

    public void changeName(String name) {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if(name.length() > 100) {
            throw new IllegalArgumentException("상품명은 100자 이하입니다.");
        }
        this.name = name;
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
    }

    public void changeDescription(String description) {
        // 상품 설명은 선택임
        if(description != null && description.length() > 2000) {
            throw new IllegalArgumentException("상품 설명은 2000자 이하입니다.");
        }
        this.description = description;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
