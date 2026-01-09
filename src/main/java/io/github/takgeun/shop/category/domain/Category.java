package io.github.takgeun.shop.category.domain;

// Domain(Entity/Model)
import lombok.Data;
import lombok.Getter;

//@Data           // @Data는 엔티티에 비추천. 왜냐하면 엔티티에는 setter가 들어가면 안되니까.
@Getter             // 엔티티에는 보통 @Getter만 두고, 변경은 의미 있는 도메인을 통해서만 진행하기.
public class Category {
    private Long id;
    private String name;
    private Long parentId;
    private boolean active;

    protected Category() {             // JPA 스펙 : 엔티티 클래스는 public 또는 protected 기본 생성자를 반드시 가져야 한다. protected를 권장. (개발자가 실수로 new 하는 걸 막기 위함)
    }

    private Category(String name, Long parentId, boolean active) {
        // Boolean : 외부 입력의 선택성을 표현
        // boolean : 도메인 상태의 확정값을 표현
        // Entity는 생성되는 순간부터 항상 유효해야 한다.

//        this.name = name;           // 이렇게 하면 생성자 생성 시 검증 로직을 넣을 수 없음.
        changeName(name);
        this.parentId = parentId;
        this.active = active;
    }

    public void assignId(Long id) {
        this.id = id;
        // 카테고리 생성 시 id가 필요한데, 엔티티에는 setter 방식으로 id를 만들 수는 없으니
        // 우선 assignId 메서드를 직접 만들고 임시로 사용할 것.
        // 추후 JPA를 통해 해결할 예정
    }

    public static Category create(String name, Long parentId) {
        // static 을 사용하는 이유
        // 1. 이름 검증은 생성자/도메인 메서드에서 반드시 수행되도록 하기 위함
        // 2. 생성 시점의 도메인 규칙을 한 곳에 고정시키게 하기 위함.
        // 비즈니스 의미가 있는 객체는 거의 다 static factory가 더 좋다.
        return new Category(name, parentId, true);
    }

    public void changeName(String name) {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다.");
        }
        if(name.length() > 50) {
            throw new IllegalArgumentException("카테고리 이름은 50자 이하입니다.");
        }
        this.name = name;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changeParent(Long parentId) {
        // 자기 자신을 부모로 못 둔다 라는 규칙은 id 생긴 뒤에 가능
        this.parentId = parentId;
    }
}
