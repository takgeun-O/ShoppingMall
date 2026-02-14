package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor        // 필수 인자를 가진 생성자 자동 생성
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // 카테고리 생성
    public Long create(String name, Long parentId) {
        String key = Category.normalizeKey(name);
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("카테고리명은 필수입니다.");
        }
        if (categoryRepository.existsByNameKey(key)) {
            throw new ConflictException("이미 존재하는 카테고리 이름입니다.");
        }

        // 부모가 있다면 부모 존재 체크
        // 카테고리 생성할 때 받은 parentId 가 존재하지 않을 경우 즉시 예외 던지기
        // 최상위 카테고리 생성할 때는 어차피 parentId가 categoryRepository에 존재하지 않으니(null) 아래 조건문은 정상 통과함.
        if (parentId != null) {
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("부모 카테고리가 존재하지 않습니다."));
        }

        Category category = Category.create(name, parentId);
        Category saved = categoryRepository.save(category);

        return saved.getId();
    }

    // 단건 조회 (유저)
    public Category getPublic(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));

        if (!category.isActive()) {
            throw new NotFoundException("카테고리가 존재하지 않습니다.");
        }

        return category;
    }

    // 단건 조회 (관리자)
    public Category getAdmin(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));
    }

    // 목록 조회 (유저)
    public List<Category> getAllPublic() {
        return categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .toList();
    }

    // 목록 조회 (관리자)
    public List<Category> getAllAdmin() {
        return categoryRepository.findAll();
    }

    // 수정 (해당 메서드는 전체 수정 PUT이 아니라 부분 수정 PATCH로 구현할 것)
    // 들어온 값만 바꾸고, 안 들어온 값은 그대로 둘거니까 여기에 들어가는 모든 파라미터 타입들은 객체 타입이어야 한다.
    // 외부 입력(PATCH/DTO/Service 경계)에서는 Boolean, 도메인 내부(Entity)에서는 boolean
    public void update(Long id, String name, Long parentId, CategoryStatus status) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));

        // name 변경만 들어온 경우
        if (name != null) {
            String key = Category.normalizeKey(name);
            if(key == null || key.isBlank()) {
                throw new IllegalArgumentException("카테고리명은 필수입니다.");
            }
            // 내 자신 제외 중복 체크 필요
            if (categoryRepository.existsByNameKeyExceptId(key, id)) {
                throw new ConflictException("이미 존재하는 카테고리 이름입니다.");
            }
            category.changeName(name);  // 원본을 넘기고 도메인에서 책임지도록
        }

        // parentId 변경만 들어온 경우
        // 부모를 null로 바꾸는 것을 허용하지 않음. (UC-C04 참고)
        if (parentId != null) {

            // 자기 자신을 부모로 지정 --> 400 Bad Request
            if (parentId.equals(id)) {
                throw new IllegalArgumentException("자기 자신을 부모로 지정할 수 없습니다.");
            }
            // 부모 존재 검증
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("부모 카테고리가 존재하지 않습니다."));

            // A -> B -> C -> A 순환부모 사이클 검증
            // newParentId에서 시작해서 parentId를 계속 따라 올라가다가 id를 만나면 순환 판정 -> 409 Conflict
            validateNoCycle(id, parentId);

            category.changeParent(parentId);
        }

        // active 변경만 들어온 경우
        if (status != null) {
            switch (status) {
                case ACTIVE -> category.activate();
                case INACTIVE -> category.deactivate();
                default -> throw new IllegalArgumentException("지원하지 않는 status 입니다.");
            }
        }

        // 메모리 저장소에서는 호출해줘야 덮어쓰기가 확실함
        categoryRepository.save(category);
    }

    // 삭제
    public void delete(Long id) {
        // 정책 : 없으면 예외 처리
        categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));

        // 하위 카테고리가 존재하거나 상품이 존재할 때 삭제 막기 (409 Conflict)
        if (categoryRepository.existsByParentId(id)) {
            throw new ConflictException("하위 카테고리가 존재하여 삭제할 수 없습니다.");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("해당 카테고리에 상품이 존재하여 삭제할 수 없습니다.");
        }

        categoryRepository.deleteById(id);
    }

    private void validateNoCycle(Long categoryId, Long newParentId) {
        Long now = newParentId;
        while (now != null) {
            if (now.equals(categoryId)) {
                throw new ConflictException("부모 카테고리 수정으로 인해 순환 구조가 발생합니다.");
            }
            Category parent = categoryRepository.findById(now)
                    .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));
            now = parent.getParentId();     // 부모의 다음 부모 넣기
        }
    }
}
