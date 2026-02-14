package io.github.takgeun.shop.category.domain;

import io.github.takgeun.shop.global.error.ConflictException;
import lombok.Getter;

@Getter             // 엔티티에는 보통 @Getter만 두고, 변경은 의미 있는 도메인을 통해서만 진행하기.
public class Category {
    private Long id;
    private String name;    // 화면 표시용 : 앞뒤 공백 trim만 사용
    private String nameKey; // 중복/검색용 키 : case-insensitive 비교용, trim + lowerCase (DB 사용 시 이 컬럼에 UNIQUE 걸기)
    private Long parentId;
    private CategoryStatus status;

    // JPA 스펙 : 엔티티 클래스는 public 또는 protected 기본 생성자를 반드시 가져야 한다. protected를 권장. (개발자가 실수로 new 하는 걸 막기 위함)
    protected Category() {
    }

    private Category(String name, Long parentId, CategoryStatus status) {
        if(status == null) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }
//        this.name = name;           // 이렇게 하면 생성자 생성 시 검증 로직을 넣을 수 없음.
        changeName(name);
        changeParent(parentId);
        this.status = status;
    }

    // 카테고리 생성 시 id가 필요한데, 엔티티에는 setter 방식으로 id를 만들 수는 없으니
    // 우선 assignId 메서드를 직접 만들고 임시로 사용할 것.
    // 추후 JPA를 통해 해결할 예정
    public void assignId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }
        if (this.id != null) {
            throw new ConflictException("id는 이미 할당되었습니다.");
        }
        if(this.parentId != null && this.parentId.equals(id)) {
            throw new IllegalArgumentException("자기 자신을 부모로 설정할 수 없습니다.");
        }
        this.id = id;
    }

    // static 을 사용하는 이유
    // 1. 이름 검증은 생성자/도메인 메서드에서 반드시 수행되도록 하기 위함
    // 2. 생성 시점의 도메인 규칙을 한 곳에 고정시키게 하기 위함.
    // 비즈니스 의미가 있는 객체는 거의 다 static factory가 더 좋다.
    public static Category create(String name, Long parentId) {
        return new Category(name, parentId, CategoryStatus.ACTIVE);
    }

    public static String normalizeDisplayName(String raw) {
        if(raw == null) return null;
        return raw.trim();
    }

    public static String normalizeKey(String raw) {
        if(raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.toLowerCase();
    }

    public void changeName(String rawName) {

        if (rawName == null) {
            throw new IllegalArgumentException("카테고리명은 필수입니다.");
        }

        String display = normalizeDisplayName(rawName);
        if(display.isEmpty()) {
            throw new IllegalArgumentException("카테고리명은 비어 있을 수 없습니다.");
        }
        if (display.length() > 50) {
            throw new IllegalArgumentException("카테고리명은 50자 이하입니다.");
        }
        this.name = display;
        this.nameKey = normalizeKey(display);
    }

    public boolean isActive() {
        return this.status == CategoryStatus.ACTIVE;
    }

    public void activate() {
        if (this.status == CategoryStatus.ACTIVE) return;
        this.status = CategoryStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == CategoryStatus.INACTIVE) return;
        this.status = CategoryStatus.INACTIVE;
    }

    public void changeParent(Long parentId) {
        if (parentId != null && parentId <= 0) {
            throw new IllegalArgumentException("parentId는 양수 또는 null이어야 합니다.");
        }
        if (this.id != null && parentId != null && parentId.equals(this.id)) {
            throw new IllegalArgumentException("자기 자신을 부모로 설정할 수 없습니다.");   // parentId 입력 자체가 잘못됨 -> 400
        }
        this.parentId = parentId;
    }


}
