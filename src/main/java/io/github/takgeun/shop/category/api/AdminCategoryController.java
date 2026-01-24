package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.dto.request.CategoryCreateRequest;
import io.github.takgeun.shop.category.dto.request.CategoryUpdateRequest;
import io.github.takgeun.shop.category.dto.response.CategoryCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    // 카테고리 생성 성공 -> 201 Created + body
    @PostMapping
    public ResponseEntity<CategoryCreateResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        Long id = categoryService.create(request.getName(), request.getParentId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new CategoryCreateResponse(id));
    }

    // 카테고리 수정 성공 -> 204 No Content
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.update(id, request.getName(), request.getParentId(), request.getActive());
        return ResponseEntity.noContent().build();
    }

    // 카테고리 삭제 -> 204 No Content
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
