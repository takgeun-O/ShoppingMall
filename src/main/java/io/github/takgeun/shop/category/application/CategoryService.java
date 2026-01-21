package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
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
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다.");
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

    // 단건 조회
    public Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));
    }

    // 목록 조회
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    // 수정 (해당 메서드는 전체 수정 PUT이 아니라 부분 수정 PATCH로 구현할 것)
    // 들어온 값만 바꾸고, 안 들어온 값은 그대로 둘거니까 여기에 들어가는 모든 파라미터 타입들은 객체 타입이어야 한다.
    // 외부 입력(PATCH/DTO/Service 경계)에서는 Boolean, 도메인 내부(Entity)에서는 boolean
    public void update(Long id, String name, Long parentId, Boolean active) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));

        // name 변경만 들어온 경우
        if(name != null) {
            // 내 자신 제외 중복 체크 필요
            boolean duplicated = categoryRepository.findAll().stream()
                    .anyMatch(c -> c.getId() != null
                            && !c.getId().equals(id)
                            && c.getName() != null
                            && c.getName().trim().equals(name.trim()));
            if(duplicated) {
                throw new IllegalArgumentException("이미 존재하는 카테고리 이름입니다.");
            }
            category.changeName(name);
        }

        // parentId 변경만 들어온 경우
        if(parentId != null) {
            // 자기 자신을 부모로 지정하면 예외 처리 (id가 null이 아닌 상황이라 가능)
            if(parentId.equals(id)) {
                throw new IllegalArgumentException("자기 자신을 부모로 지정할 수 없습니다.");
            }

            // 부모 카테고리가 존재하지 않을 경우 예외 처리
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("부모 카테고리가 존재하지 않습니다."));

            category.changeParent(parentId);
        }

        // active 변경만 들어온 경우
        if(active != null) {
            if(active) {
                category.activate();
            } else {
                category.deactivate();
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
        if(categoryRepository.existsByParentId(id)) {
            throw new ConflictException("하위 카테고리가 존재하여 삭제할 수 없습니다.");
        }
        if(productRepository.existsByCategoryId(id)) {
            throw new ConflictException("해당 카테고리에 상품이 존재하여 삭제할 수 없습니다.");
        }

        categoryRepository.deleteById(id);
    }
}
