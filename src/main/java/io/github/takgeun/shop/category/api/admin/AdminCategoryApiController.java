package io.github.takgeun.shop.category.api.admin;

import io.github.takgeun.shop.category.api.admin.dto.request.CreateCategoryRequest;
import io.github.takgeun.shop.category.api.admin.dto.request.UpdateCategoryRequest;
import io.github.takgeun.shop.category.api.admin.dto.request.UpdateCategoryStatusRequest;
import io.github.takgeun.shop.category.api.admin.dto.response.AdminCategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.global.api.ApiController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@ApiController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryApiController {

    private final CategoryService categoryService;

    @GetMapping("/{categoryId}")
    public AdminCategoryResponse findOne(
            @PathVariable @Positive(message = "카테고리 ID는 양수여야 합니다.")
            Long categoryId
    ) {
        Category category = categoryService.getAdmin(categoryId);

        return AdminCategoryResponse.from(category);
    }

    @GetMapping
    public List<AdminCategoryResponse> findAll() {
        return categoryService.getAllAdmin()
                .stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AdminCategoryResponse> create(
            @Valid @RequestBody
            CreateCategoryRequest request
    ) {
        Long categoryId = categoryService.create(
                request.name(),
                request.parentId()
        );

        // 생성 후 한번 더 조회 --> 생성된 ID와 정규화된 name, slug, status를 응답하려면 필요한 과정임.
        Category category = categoryService.getAdmin(categoryId);

        URI location = URI.create(
                "/api/v1/admin/categories/" + categoryId
        );

        /**
         * ResponseEntity.created(location)
         * 리소스 생성 성공을 뜻하는 201 Created 상태와 새로 생성된 리소스 주소를 함께 응답하기 위해 사용
         *
         * HTTP/1.1 201 Created
         * Location: /api/v1/admin/categories/15
         * Content-Type: application/json
         */
        return ResponseEntity
                .created(location)
                .body(AdminCategoryResponse.from(category));
    }

    /**
     * 기본 정보 수정
     * PUT /api/v1/admin/categories/{categoryId}
     * → name, parentId
     *
     * 운영 상태 변경 (일부만 받으면 되니까 PATCH)
     * PATCH /api/v1/admin/categories/{categoryId}/status
     * → status
     */
    @PutMapping(
            value = "/{categoryId}"
    )
    public AdminCategoryResponse update(
            @PathVariable @Positive(message = "카테고리 ID는 양수여야 합니다.") Long categoryId,
            @Valid @RequestBody
            UpdateCategoryRequest request
    ) {
        categoryService.update(
                categoryId,
                request.name(),
                request.parentId()
        );

        Category updatedCategory = categoryService.getAdmin(categoryId);

        return AdminCategoryResponse.from(updatedCategory);
    }

    @PatchMapping(
            value = "/{categoryId}/status")
    public AdminCategoryResponse updateStatus(
            @PathVariable @Positive(message = "카테고리 ID는 양수여야 합니다.")
            Long categoryId,

            @Valid @RequestBody
            UpdateCategoryStatusRequest request
    ) {
        categoryService.changeStatus(
                categoryId,
                request.status()
        );

        Category updatedCategory = categoryService.getAdmin(categoryId);

        return AdminCategoryResponse.from(updatedCategory);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "카테고리 ID는 양수여야 합니다.")
            Long categoryId
    ) {
        categoryService.delete(categoryId);

        return ResponseEntity.noContent().build();
    }
}
