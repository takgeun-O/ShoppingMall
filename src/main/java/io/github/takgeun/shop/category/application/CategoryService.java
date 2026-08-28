package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.category.view.dto.admin.AdminCategoryEditView;
import io.github.takgeun.shop.category.view.dto.admin.CategoryOptionView;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 도메인 규칙 + 쓰기 작업 중심
 * - 카테고리 생성
 * - 카테고리 수정
 * - 카테고리 삭제
 * - 부모 유효성 검증
 * - 이름 중복 검증
 * - 순환 참조 검증
 * - 삭제 가능 조건 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)     // 조회가 많은 서비스는 이걸로. (대신 쓰기 메서드는 @Transactional로 열기)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * 카테고리 생성
     * 부모 존재 여부 확인
     * 2단계까지만!
     * 이름/slug 중복 금지
     */
    @Transactional
    public Long create(String name, Long parentId) {
        Category parent = validateCreatableParent(parentId);    // 카테고리 생성 시 2단까지 허용하기

        String normalizedKey = Category.normalizeKey(name);
        validateDuplicateName(normalizedKey);

        String slug = Category.createSlug(name);
        validateDuplicateSlug(slug);

        Category category = Category.create(name, slug, parent != null ? parent.getId() : null);    // 관리자가 입력한 그대로 name 써서 카테고리 생성
        Category saved = categoryRepository.save(category);
        return saved.getId();
    }


    /**
     * 카테고리 수정
     * - 부모 지정 없음
     * - 자기 자신인가?
     * - 부모가 실제로 존재하는가?
     * - 후손을 부모로 지정했는가?
     * - 최대 깊이를 위반하는가?
     * 순으로 검증
     */
    @Transactional
    public void update(Long categoryId, String name, Long parentId) {

        Category category = getCategoryOrThrow(categoryId);

        validateNoSelfParent(categoryId, parentId);     // 자기 자신을 부모로 설정할 수 없음

        validateNoCircularReference(categoryId, parentId);      // 순환 참조 여부 "A면 B이고 B가 C이면 A는 C이다." 금지

        Category parent = validateUpdatableParent(categoryId, parentId);        // 부모 카테고리 존재 여부 확인 + 자기 자신 부모 설정 금지 + 2단 금지

        String normalizedKey = Category.normalizeKey(name);
        validateDuplicateNameForUpdate(normalizedKey, categoryId);  // 카테고리 수정할 때 나 자신을 제외한 카테고리명 중복이 있는지 체크

        String newSlug = Category.createSlug(name);
//        validateDuplicateSlug(newSlug);
        validateDuplicateSlugForUpdate(newSlug, categoryId);

        category.changeName(name);
        category.changeSlug(newSlug);
        category.changeParent(parentId != null ? parent.getId() : null);

        categoryRepository.save(category);
    }

    /**
     * 카테고리 삭제
     * 하위 카테고리 존재 시 삭제 불가
     * 연결된 상품 존재 시 삭제 불가
     */
    @Transactional
    public void delete(Long categoryId) {
        getCategoryOrThrow(categoryId);
        validateNoChildren(categoryId);
        validateNoProducts(categoryId);

        categoryRepository.deleteById(categoryId);
    }

    // 헤더 상단 대표 카테고리
    public List<CategoryResponse> getTopCategories() {
        // 1뎁스(parentId==null) 중에서 상단에 노출할 것만
        return categoryRepository.findAllPublic().stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparing(Category::getName))
                .map(CategoryResponse::from)
                .toList();
    }

    // 전체 카테고리(트리용) - 공개용
    public List<CategoryResponse> getAllPublicCategories() {
        return categoryRepository.findAllPublic().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    // 전체 카테고리(트리용) - 관리자용
    public List<CategoryResponse> getAllAdminCategories() {
        return categoryRepository.findAllAdmin().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    // 단건 조회 (유저)
    public Category getPublic(Long id) {
        Category category = getCategoryOrThrow(id);
        if (!category.isActive()) {
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return category;
    }

    // 단건 조회 (관리자)
    public Category getAdmin(Long id) {
        return getCategoryOrThrow(id);
    }

    // 목록 조회 (유저)
    public List<Category> getAllPublic() {
        return categoryRepository.findAllPublic().stream()
                .filter(Category::isPublicVisible)
                .toList();
    }

    // 목록 조회 (관리자)
    public List<Category> getAllAdmin() {
        return categoryRepository.findAllAdmin();
    }

    // 카테고리 옵션 조회 메서드
    public List<CategoryOptionView> findAllForAdminSelect() {
        return categoryRepository.findAllAdmin().stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(category -> CategoryOptionView.of(category.getId(), category.getName()))
                .toList();
    }

    public Set<Long> findPublicDescendantIdsIncludingSelf(Long rootId) {
        // 공개 카테고리 전체 가져오기
        List<CategoryResponse> all = getAllPublicCategories();
        log.info("allPublic.size={}", all.size());

        // 공개 카테고리 전체 중 사용자가 선택한 카테고리와 그 하위 카테고리 추려내기
        return findDescendantIdsIncludingSelf(all, rootId);
    }

    public Set<Long> findAdminDescendantIdsIncludingSelf(Long rootId) {
        // 모든 카테고리 정보 가져오기
        List<CategoryResponse> all = getAllAdminCategories();
        log.info("allAdmin.size={}", all.size());

        // rootId와 그 자식 집합 반환
        return findDescendantIdsIncludingSelf(all, rootId);
    }

    public String findAdminNameOrNull(Long categoryId) {

        if (categoryId == null) return null;

        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse(null);
    }

    public String findPublicNameOrNull(Long categoryId) {
        if (categoryId == null) return null;

        return categoryRepository.findById(categoryId)
                .filter(Category::isPublicVisible)
                .map(Category::getName)
                .orElse(null);
    }

    public AdminCategoryEditView getAdminCategoryEditView(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        String parentName = null;
        if(category.getParentId() != null) {
            Category parent = categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> new IllegalStateException("카테고리의 부모 참조가 유효하지 않습니다."));   // 데이터가 꼬여서 parentId는 있는데 부모가 실제로 없는 데이터일 경우 방지
            parentName = parent.getName();
        }

        return AdminCategoryEditView.of(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParentId(),
                parentName
        );
    }


    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateDuplicateName(String normalizedKey) {
        if (normalizedKey == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (categoryRepository.existsByNameKey(normalizedKey)) {
            throw new ConflictException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }

    private void validateDuplicateNameForUpdate(String normalizedKey, Long categoryId) {
        if (normalizedKey == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (categoryRepository.existsByNameKeyExceptId(normalizedKey, categoryId)) {
            // 나 자신을 제외한 카테고리명이 있는지 여부 체크
            throw new ConflictException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }

    private void validateDuplicateSlug(String slug) {
        if(slug == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if(categoryRepository.existsBySlug(slug)) {
            throw new ConflictException(ErrorCode.CATEGORY_SLUG_DUPLICATED);
        }
    }

    private void validateDuplicateSlugForUpdate(String slug, Long categoryId) {
        if(slug == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if(categoryRepository.existsBySlugExceptId(slug, categoryId)) {
            throw new ConflictException(ErrorCode.CATEGORY_SLUG_DUPLICATED);
        }
    }

    private void validateNoSelfParent(Long categoryId, Long parentId) {
        if(parentId == null) {
            return;
        }

        if(categoryId.equals(parentId)) {
            throw new BusinessException(ErrorCode.INVALID_CATEGORY_PARENT);
        }
    }

    private void validateNoCircularReference(Long categoryId, Long parentId) {
        if (parentId == null) {
            return;
        }

        Set<Long> visited = new HashSet<>();
        Long currentId = parentId;

        while (currentId != null) {
            if (currentId.equals(categoryId)) {
                // A가 B면 B는 A다.
                throw new ConflictException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE);
            }
            if (!visited.add(currentId)) {
                // A -> B, B -> C, .... ? -> A~(이미 방문한 카테고리)
                throw new IllegalStateException("저장된 카테고리 계층에 순환 참조가 존재합니다.");  // 이미 저장된 데이터 자체에 순환이 있다는 뜻으로 서버 내부 오류로 봐야함. 클라이언트에서는 공통 메시지로 처리할 것.
            }

            Category current = categoryRepository.findById(currentId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PARENT_CATEGORY_NOT_FOUND));

            currentId = current.getParentId();
        }
    }

    private void validateNoChildren(Long categoryId) {
        if (categoryRepository.existsByParentId(categoryId)) {
            throw new ConflictException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
    }

    private void validateNoProducts(Long categoryId) {
        if(productRepository.existsAdminByCategoryId(categoryId)) {
            throw new ConflictException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
    }

    private Category validateCreatableParent(Long parentId) {
        // 카테고리 생성 시 2단까지 허용하기
        if(parentId == null) {
            return null;
        }

        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PARENT_CATEGORY_NOT_FOUND));

        // 부모가 이미 하위 카테고리라면 여기에 자식을 달 경우 3단이 되어버림
        if(parent.getParentId() != null) {
            throw new BusinessException(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
        }

        return parent;
    }

    private Category validateUpdatableParent(Long categoryId, Long parentId) {
        if(parentId == null) {
            return null;
        }

        Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PARENT_CATEGORY_NOT_FOUND));

        if(parent.getId().equals(categoryId)) {
            throw new BusinessException(ErrorCode.INVALID_CATEGORY_PARENT);
        }
        if(parent.getParentId() != null) {
            throw new BusinessException(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
        }

        return parent;
    }

    private Set<Long> findDescendantIdsIncludingSelf(List<CategoryResponse> all, Long rootId) {

        // parentId -> children 맵
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (CategoryResponse c : all) {
            childrenByParent
                    .computeIfAbsent(c.getParentId(), k -> new ArrayList<>())
                    .add(c.getId());
        }

        // BFS (부모 -> 자식으로 가는 순서가 자연스러움)
        Set<Long> result = new LinkedHashSet<>();   // 순서 유지 + 중복 제거
        Deque<Long> q = new ArrayDeque<>();

        q.add(rootId);
        result.add(rootId);     // visited 역할도 겸함.

        while(!q.isEmpty()) {
            Long cur = q.poll();

            for (Long childId : childrenByParent.getOrDefault(cur, List.of())) {
                if(result.add(childId)) {
                    q.add(childId);
                }
            }
        }

        return result;
    }
}
